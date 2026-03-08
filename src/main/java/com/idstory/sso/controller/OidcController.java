package com.idstory.sso.controller;

import com.idstory.sso.dto.OidcTokenResponse;
import com.idstory.sso.entity.SsoAuthCode;
import com.idstory.sso.entity.SsoClient;
import com.idstory.sso.entity.SsoToken;
import com.idstory.sso.service.*;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OIDC Authorization Server 엔드포인트
 *
 * <ul>
 *   <li>GET  /oauth2/authorize          — Authorization Code 발급</li>
 *   <li>POST /oauth2/token              — 토큰 교환</li>
 *   <li>GET  /oauth2/userinfo           — UserInfo (Bearer 토큰)</li>
 *   <li>GET  /.well-known/openid-configuration — Discovery</li>
 *   <li>GET  /oauth2/jwks               — JWKS (공개키)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OidcController {

    private final SsoClientService  ssoClientService;
    private final SsoAuthCodeService authCodeService;
    private final SsoTokenService   tokenService;
    private final SsoAccessLogService accessLogService;
    private final SsoKeyPairService keyPairService;
    private final SysUserRepository userRepository;

    @Value("${sso.issuer:http://localhost:9091}")
    private String issuer;

    // ── Authorization Endpoint ───────────────────────────────────────────────

    /**
     * GET /oauth2/authorize
     * - 인증 필요 (Spring Security가 미로그인 시 /login으로 리다이렉트 후 복귀)
     */
    @GetMapping("/oauth2/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam("client_id")     String clientId,
            @RequestParam("redirect_uri")  String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope",  defaultValue = "openid") String scope,
            @RequestParam(value = "state",  required = false)        String state,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        String ip = extractIp(request);

        // response_type 검증
        if (!"code".equals(responseType)) {
            return errorRedirect(redirectUri, state, "unsupported_response_type", "response_type must be 'code'");
        }

        SsoClient client;
        try {
            client = ssoClientService.findByClientId(clientId);
        } catch (Exception e) {
            accessLogService.log(null, clientId, null, null, ip, "AUTHORIZE", "FAIL", e.getMessage());
            return errorRedirect(redirectUri, state, "invalid_client", e.getMessage());
        }

        // use_yn 체크
        if (!"Y".equals(client.getUseYn())) {
            accessLogService.log(client.getClientOid(), clientId, null, null, ip, "AUTHORIZE", "DENIED", "비활성 클라이언트");
            return errorRedirect(redirectUri, state, "access_denied", "클라이언트가 비활성 상태입니다.");
        }

        // redirect_uri 검증
        if (!isValidRedirectUri(client, redirectUri)) {
            accessLogService.log(client.getClientOid(), clientId, null, null, ip, "AUTHORIZE", "FAIL", "redirect_uri 불일치");
            return errorRedirect(redirectUri, state, "invalid_request", "등록되지 않은 redirect_uri입니다.");
        }

        // 사용자 조회
        SysUser user = userRepository.findByUserId(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자 조회 실패: " + principal.getUsername()));

        // 인증코드 발급
        SsoAuthCode code = authCodeService.issue(client, user, redirectUri, scope, state, ip);
        accessLogService.log(client.getClientOid(), clientId, user.getOid(), user.getUserId(), ip, "AUTHORIZE", "SUCCESS", null);

        // redirect_uri?code=...&state=...
        String location = buildRedirectUri(redirectUri, code.getAuthCode(), state);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    // ── Token Endpoint ───────────────────────────────────────────────────────

    /**
     * POST /oauth2/token
     * - public 엔드포인트 (Authorization: Basic base64(client_id:client_secret))
     */
    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> token(
            @RequestParam("grant_type")   String grantType,
            @RequestParam(value = "code",          required = false) String code,
            @RequestParam(value = "redirect_uri",  required = false) String redirectUri,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request) {

        String ip = extractIp(request);
        String[] credentials = extractBasicAuth(request);
        if (credentials == null) {
            return oidcError("invalid_client", "Authorization 헤더가 없거나 형식이 잘못되었습니다.", HttpStatus.UNAUTHORIZED);
        }

        String clientId     = credentials[0];
        String clientSecret = credentials[1];

        SsoClient client;
        try {
            client = ssoClientService.verifySecret(clientId, clientSecret);
        } catch (Exception e) {
            accessLogService.log(null, clientId, null, null, ip, "TOKEN", "FAIL", e.getMessage());
            return oidcError("invalid_client", e.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        if ("authorization_code".equals(grantType)) {
            if (code == null || redirectUri == null) {
                return oidcError("invalid_request", "code와 redirect_uri는 필수입니다.", HttpStatus.BAD_REQUEST);
            }
            SsoAuthCode authCode;
            try {
                authCode = authCodeService.consume(code, clientId, redirectUri);
            } catch (Exception e) {
                accessLogService.log(client.getClientOid(), clientId, null, null, ip, "TOKEN", "FAIL", e.getMessage());
                return oidcError("invalid_grant", e.getMessage(), HttpStatus.BAD_REQUEST);
            }

            SysUser user = userRepository.findById(authCode.getUserOid())
                    .orElseThrow(() -> new IllegalStateException("사용자 조회 실패"));

            SsoTokenService.IssuedTokens tokens = tokenService.issue(client, authCode, user, ip);
            accessLogService.log(client.getClientOid(), clientId, user.getOid(), user.getUserId(), ip, "TOKEN", "SUCCESS", null);

            return ResponseEntity.ok(OidcTokenResponse.builder()
                    .accessToken(tokens.accessToken())
                    .tokenType("Bearer")
                    .expiresIn(tokens.expiresIn())
                    .refreshToken(tokens.refreshToken())
                    .idToken(tokens.idToken())
                    .scope(authCode.getScopes())
                    .build());
        }

        return oidcError("unsupported_grant_type", "지원하지 않는 grant_type: " + grantType, HttpStatus.BAD_REQUEST);
    }

    // ── UserInfo Endpoint ────────────────────────────────────────────────────

    /**
     * GET /oauth2/userinfo
     * - Bearer 토큰 검증 후 사용자 클레임 반환
     */
    @GetMapping("/oauth2/userinfo")
    public ResponseEntity<?> userinfo(HttpServletRequest request) {
        String ip = extractIp(request);
        String bearerToken = extractBearerToken(request);
        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                    .body(Map.of("error", "invalid_token", "error_description", "Bearer 토큰이 없습니다."));
        }

        SysUser user;
        try {
            user = tokenService.validateAccessToken(bearerToken);
        } catch (Exception e) {
            accessLogService.log(null, null, null, null, ip, "USERINFO", "FAIL", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                    .body(Map.of("error", "invalid_token", "error_description", e.getMessage()));
        }

        accessLogService.log(null, null, user.getOid(), user.getUserId(), ip, "USERINFO", "SUCCESS", null);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub",                user.getOid());
        claims.put("name",               user.getName());
        claims.put("preferred_username", user.getUserId());
        claims.put("email",              user.getEmail());
        return ResponseEntity.ok(claims);
    }

    // ── Discovery Endpoint ───────────────────────────────────────────────────

    @GetMapping("/.well-known/openid-configuration")
    public ResponseEntity<Map<String, Object>> discovery(HttpServletRequest request) {
        accessLogService.log(null, null, null, null, extractIp(request), "DISCOVERY", "SUCCESS", null);
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("issuer",                                issuer);
        doc.put("authorization_endpoint",               issuer + "/oauth2/authorize");
        doc.put("token_endpoint",                        issuer + "/oauth2/token");
        doc.put("userinfo_endpoint",                     issuer + "/oauth2/userinfo");
        doc.put("jwks_uri",                              issuer + "/oauth2/jwks");
        doc.put("response_types_supported",              new String[]{"code"});
        doc.put("subject_types_supported",               new String[]{"public"});
        doc.put("id_token_signing_alg_values_supported", new String[]{"RS256"});
        doc.put("scopes_supported",                      new String[]{"openid", "profile", "email"});
        doc.put("token_endpoint_auth_methods_supported", new String[]{"client_secret_basic"});
        doc.put("grant_types_supported",                 new String[]{"authorization_code"});
        doc.put("claims_supported",                      new String[]{"sub", "name", "preferred_username", "email"});
        return ResponseEntity.ok(doc);
    }

    // ── JWKS Endpoint ────────────────────────────────────────────────────────

    @GetMapping("/oauth2/jwks")
    public ResponseEntity<Map<String, Object>> jwks(HttpServletRequest request) {
        accessLogService.log(null, null, null, null, extractIp(request), "JWKS", "SUCCESS", null);
        RSAPublicKey pub = keyPairService.getPublicKey();
        String kid = keyPairService.getKeyId();

        // Base64url 인코딩 (패딩 없음)
        String n = base64url(pub.getModulus().toByteArray());
        String e = base64url(pub.getPublicExponent().toByteArray());

        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("kid", kid);
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("n",   n);
        jwk.put("e",   e);

        return ResponseEntity.ok(Map.of("keys", new Object[]{jwk}));
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private boolean isValidRedirectUri(SsoClient client, String redirectUri) {
        if (client.getRedirectUris() == null) return false;
        return Arrays.stream(client.getRedirectUris().split("[\r\n,]+"))
                .map(String::trim)
                .anyMatch(uri -> uri.equals(redirectUri));
    }

    private String buildRedirectUri(String base, String code, String state) {
        String url = base + (base.contains("?") ? "&" : "?") + "code=" + code;
        if (state != null && !state.isBlank()) url += "&state=" + state;
        return url;
    }

    private ResponseEntity<Void> errorRedirect(String redirectUri, String state, String error, String desc) {
        String url = redirectUri + (redirectUri.contains("?") ? "&" : "?")
                + "error=" + error + "&error_description=" + desc;
        if (state != null && !state.isBlank()) url += "&state=" + state;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    private ResponseEntity<Map<String, Object>> oidcError(String error, String desc, HttpStatus status) {
        return ResponseEntity.status(status).body(Map.of("error", error, "error_description", desc));
    }

    private String[] extractBasicAuth(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) return null;
            return new String[]{decoded.substring(0, colon), decoded.substring(colon + 1)};
        } catch (Exception e) {
            return null;
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return request.getRemoteAddr();
    }

    private String base64url(byte[] bytes) {
        // BigInteger.toByteArray()는 부호 비트로 인해 0x00 앞에 붙을 수 있어 제거
        int start = 0;
        while (start < bytes.length - 1 && bytes[start] == 0) start++;
        byte[] trimmed = (start == 0) ? bytes : Arrays.copyOfRange(bytes, start, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(trimmed);
    }
}
