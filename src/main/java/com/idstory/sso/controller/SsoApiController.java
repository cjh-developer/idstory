package com.idstory.sso.controller;

import com.idstory.policy.service.SystemPolicyService;
import com.idstory.sso.dto.OidcTokenResponse;
import com.idstory.sso.entity.SsoAuthCode;
import com.idstory.sso.entity.SsoClient;
import com.idstory.sso.service.*;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSO API 엔드포인트 (커스텀 /sso/* 경로)
 *
 * <ul>
 *   <li>GET  /sso/auth      — Authorization Code 발급 (인증 필요)</li>
 *   <li>POST /sso/token     — 토큰 발급 (authorization_code / refresh_token)</li>
 *   <li>GET  /sso/userinfo  — 사용자 정보 (Bearer + client_id/secret 쿼리)</li>
 *   <li>POST /sso/logout    — 로그아웃 (해당 client+user 전체 토큰 취소)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SsoApiController {

    private final SsoClientService    ssoClientService;
    private final SsoAuthCodeService  authCodeService;
    private final SsoTokenService     tokenService;
    private final SsoAccessLogService accessLogService;
    private final JwtTokenService     jwtTokenService;
    private final SysUserRepository   userRepository;
    private final SystemPolicyService policyService;

    // ── Authorization Endpoint ───────────────────────────────────────────────

    /**
     * GET /sso/auth
     * - 인증 필요 (미로그인 시 Spring Security → /login 리다이렉트)
     */
    @GetMapping("/sso/auth")
    public ResponseEntity<Void> auth(
            @RequestParam("client_id")     String clientId,
            @RequestParam("redirect_uri")  String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope",  defaultValue = "openid") String scope,
            @RequestParam(value = "state",  required = false)        String state,
            Authentication authentication,
            HttpServletRequest request) {

        String ip = extractIp(request);

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

        if (!"Y".equals(client.getUseYn())) {
            accessLogService.log(client.getClientOid(), clientId, null, null, ip, "AUTHORIZE", "DENIED", "비활성 클라이언트");
            return errorRedirect(redirectUri, state, "access_denied", "클라이언트가 비활성 상태입니다.");
        }

        if (!isValidRedirectUri(client, redirectUri)) {
            accessLogService.log(client.getClientOid(), clientId, null, null, ip, "AUTHORIZE", "FAIL", "redirect_uri 불일치");
            return errorRedirect(redirectUri, state, "invalid_request", "등록되지 않은 redirect_uri입니다.");
        }

        SysUser user = userRepository.findByUserId(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("사용자 조회 실패: " + authentication.getName()));

        SsoAuthCode code = authCodeService.issue(client, user, redirectUri, scope, state, ip);
        accessLogService.log(client.getClientOid(), clientId, user.getOid(), user.getUserId(), ip, "AUTHORIZE", "SUCCESS", null);

        String location = buildRedirectUri(redirectUri, code.getAuthCode(), state);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    // ── Token Endpoint ───────────────────────────────────────────────────────

    /**
     * POST /sso/token — application/json
     */
    @PostMapping(value = "/sso/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> tokenJson(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        return processToken(
                body.get("grant_type"),
                body.get("client_id"),
                body.get("client_secret"),
                body.get("code"),
                body.get("redirect_uri"),
                body.get("refresh_token"),
                extractIp(request));
    }

    /**
     * POST /sso/token — application/x-www-form-urlencoded
     */
    @PostMapping(value = "/sso/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> tokenForm(
            @RequestParam("grant_type")                          String grantType,
            @RequestParam("client_id")                           String clientId,
            @RequestParam("client_secret")                       String clientSecret,
            @RequestParam(value = "code",          required = false) String code,
            @RequestParam(value = "redirect_uri",  required = false) String redirectUri,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            HttpServletRequest request) {

        return processToken(grantType, clientId, clientSecret, code, redirectUri, refreshToken, extractIp(request));
    }

    private ResponseEntity<?> processToken(String grantType, String clientId, String clientSecret,
                                           String code, String redirectUri, String refreshToken,
                                           String ip) {
        if (grantType == null) {
            return ssoError("invalid_request", "grant_type은 필수입니다.", HttpStatus.BAD_REQUEST);
        }

        SsoClient client;
        try {
            client = ssoClientService.verifySecret(clientId, clientSecret);
        } catch (Exception e) {
            accessLogService.log(null, clientId, null, null, ip, "TOKEN", "FAIL", e.getMessage());
            return ssoError("invalid_client", e.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        if ("authorization_code".equals(grantType)) {
            if (code == null || redirectUri == null) {
                return ssoError("invalid_request", "code와 redirect_uri는 필수입니다.", HttpStatus.BAD_REQUEST);
            }
            SsoAuthCode authCode;
            try {
                authCode = authCodeService.consume(code, clientId, redirectUri);
            } catch (Exception e) {
                accessLogService.log(client.getClientOid(), clientId, null, null, ip, "TOKEN", "FAIL", e.getMessage());
                return ssoError("invalid_grant", e.getMessage(), HttpStatus.BAD_REQUEST);
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

        if ("refresh_token".equals(grantType)) {
            if (refreshToken == null || refreshToken.isBlank()) {
                return ssoError("invalid_request", "refresh_token은 필수입니다.", HttpStatus.BAD_REQUEST);
            }
            SsoTokenService.IssuedTokens tokens;
            try {
                tokens = tokenService.issueFromRefresh(refreshToken, client, ip);
            } catch (Exception e) {
                accessLogService.log(client.getClientOid(), clientId, null, null, ip, "TOKEN", "FAIL", e.getMessage());
                return ssoError("invalid_grant", e.getMessage(), HttpStatus.BAD_REQUEST);
            }
            accessLogService.log(client.getClientOid(), clientId, null, null, ip, "TOKEN", "SUCCESS", "refresh");

            return ResponseEntity.ok(OidcTokenResponse.builder()
                    .accessToken(tokens.accessToken())
                    .tokenType("Bearer")
                    .expiresIn(tokens.expiresIn())
                    .refreshToken(tokens.refreshToken())
                    .idToken(tokens.idToken())
                    .build());
        }

        return ssoError("unsupported_grant_type", "지원하지 않는 grant_type: " + grantType, HttpStatus.BAD_REQUEST);
    }

    // ── UserInfo Endpoint ────────────────────────────────────────────────────

    /**
     * POST /sso/userinfo — application/x-www-form-urlencoded
     * - Authorization: Bearer {token}
     * - client_id, client_secret (form body)
     */
    @PostMapping(value = "/sso/userinfo", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> userinfo(
            @RequestParam("client_id")     String clientId,
            @RequestParam("client_secret") String clientSecret,
            HttpServletRequest request) {

        String ip = extractIp(request);

        // client 인증
        try {
            ssoClientService.verifySecret(clientId, clientSecret);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client", "error_description", e.getMessage()));
        }

        // Bearer 토큰 검증
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
            accessLogService.log(null, clientId, null, null, ip, "USERINFO", "FAIL", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                    .body(Map.of("error", "invalid_token", "error_description", e.getMessage()));
        }

        accessLogService.log(null, clientId, user.getOid(), user.getUserId(), ip, "USERINFO", "SUCCESS", null);

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub",                user.getOid());
        claims.put("name",               user.getName());
        claims.put("preferred_username", user.getUserId());
        claims.put("email",              user.getEmail());
        return ResponseEntity.ok(claims);
    }

    // ── Logout Endpoint ──────────────────────────────────────────────────────

    /**
     * POST /sso/logout — application/x-www-form-urlencoded
     * - Authorization: Bearer {token}
     * - client_id, client_secret (form body)
     * - 해당 client + user 전체 활성 토큰 일괄 취소
     */
    @PostMapping(value = "/sso/logout", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> logout(
            @RequestParam("client_id")     String clientId,
            @RequestParam("client_secret") String clientSecret,
            HttpServletRequest request) {

        String ip = extractIp(request);

        // client 인증
        SsoClient client;
        try {
            client = ssoClientService.verifySecret(clientId, clientSecret);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "invalid_client", "error_description", e.getMessage()));
        }

        // Bearer 토큰에서 userOid 추출
        String bearerToken = extractBearerToken(request);
        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "invalid_token", "error_description", "Bearer 토큰이 없습니다."));
        }

        String userOid;
        try {
            Claims claims = jwtTokenService.parse(bearerToken);
            userOid = claims.getSubject();
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "invalid_token", "error_description", "유효하지 않은 토큰입니다."));
        }

        tokenService.revokeAllByClientAndUser(client.getClientOid(), userOid);
        accessLogService.log(client.getClientOid(), clientId, userOid, null, ip, "LOGOUT", "SUCCESS", null);

        return ResponseEntity.ok(Map.of("success", true, "message", "로그아웃 되었습니다."));
    }

    // ── Login Check / Token Extend Endpoint ─────────────────────────────────

    /**
     * POST /sso/check — application/x-www-form-urlencoded
     * - Authorization: Bearer {access_token}
     * - client_id, client_secret (form body)
     * - 현재 Access Token 검증 후 유효시간 연장된 새 Access Token 발급
     * - 연장 시간: SYSTEM_POLICY.SSO_ACCESS_TOKEN_EXTEND_SEC (기본 1800초)
     */
    @PostMapping(value = "/sso/check", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> check(
            @RequestParam("client_id")     String clientId,
            @RequestParam("client_secret") String clientSecret,
            HttpServletRequest request) {

        String ip = extractIp(request);

        // client 인증
        SsoClient client;
        try {
            client = ssoClientService.verifySecret(clientId, clientSecret);
        } catch (Exception e) {
            accessLogService.log(null, clientId, null, null, ip, "CHECK", "FAIL", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "invalid_client", "error_description", e.getMessage()));
        }

        // Bearer 토큰 추출
        String bearerToken = extractBearerToken(request);
        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "invalid_token", "error_description", "Bearer 토큰이 없습니다."));
        }

        // 정책에서 연장 시간 조회 (기본 1800초 = 30분)
        int extendSec = policyService.getInt("SYSTEM_POLICY", "SSO_ACCESS_TOKEN_EXTEND_SEC", 1800);

        // Access Token 검증 + 연장
        SsoTokenService.ExtendedToken extended;
        try {
            extended = tokenService.extendAccessToken(bearerToken, client, extendSec, ip);
        } catch (Exception e) {
            accessLogService.log(client.getClientOid(), clientId, null, null, ip, "CHECK", "FAIL", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "invalid_token", "error_description", e.getMessage()));
        }

        SysUser user = extended.user();
        accessLogService.log(client.getClientOid(), clientId, user.getOid(), user.getUserId(), ip, "CHECK", "SUCCESS", null);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("valid",        true);
        resp.put("access_token", extended.accessToken());
        resp.put("token_type",   "Bearer");
        resp.put("expires_in",   extended.expiresIn());
        resp.put("user", Map.of(
                "sub",                user.getOid(),
                "name",               user.getName(),
                "preferred_username", user.getUserId(),
                "email",              user.getEmail() != null ? user.getEmail() : ""
        ));
        return ResponseEntity.ok(resp);
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

    private ResponseEntity<Map<String, Object>> ssoError(String error, String desc, HttpStatus status) {
        return ResponseEntity.status(status).body(Map.of("error", error, "error_description", desc));
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
}
