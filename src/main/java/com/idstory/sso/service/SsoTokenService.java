package com.idstory.sso.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.sso.entity.SsoAuthCode;
import com.idstory.sso.entity.SsoClient;
import com.idstory.sso.entity.SsoToken;
import com.idstory.sso.repository.SsoTokenRepository;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SSO 토큰 발급/취소/조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoTokenService {

    private final SsoTokenRepository  tokenRepository;
    private final JwtTokenService     jwtTokenService;
    private final SysUserRepository   userRepository;

    public record IssuedTokens(String accessToken, String refreshToken, String idToken,
                               int expiresIn) {}

    public record ExtendedToken(String accessToken, int expiresIn, SysUser user) {}

    /**
     * Authorization Code 교환으로 토큰 발급 (ACCESS + REFRESH + ID)
     */
    @Transactional
    public IssuedTokens issue(SsoClient client, SsoAuthCode authCode, SysUser user, String ip) {
        String scopes = authCode.getScopes() != null ? authCode.getScopes() : "openid";

        String accessJti  = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        String idJti      = UUID.randomUUID().toString();

        String accessToken  = jwtTokenService.createAccessToken(user, client, scopes, accessJti);
        String refreshToken = jwtTokenService.createRefreshToken(user, client, refreshJti);
        String idToken      = jwtTokenService.createIdToken(user, client, idJti);

        LocalDateTime now = LocalDateTime.now();
        save(client, user, "ACCESS",  accessJti,  scopes, now.plusSeconds(client.getAccessTokenValiditySec()),  ip);
        save(client, user, "REFRESH", refreshJti, scopes, now.plusSeconds(client.getRefreshTokenValiditySec()), ip);
        save(client, user, "ID",      idJti,      scopes, now.plusSeconds(client.getIdTokenValiditySec()),      ip);

        log.info("[SSO] 토큰 발급 - clientId={}, userId={}", client.getClientId(), user.getUserId());
        return new IssuedTokens(accessToken, refreshToken, idToken, client.getAccessTokenValiditySec());
    }

    /**
     * Bearer access_token 검증 후 사용자 반환
     */
    public SysUser validateAccessToken(String rawToken) {
        Claims claims;
        try {
            claims = jwtTokenService.parse(rawToken);
        } catch (JwtException e) {
            throw new IllegalArgumentException("유효하지 않은 Access Token: " + e.getMessage());
        }

        String jti = claims.getId();
        SsoToken record = tokenRepository.findByJti(jti)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 토큰입니다."));
        if (record.getRevokedAt() != null) {
            throw new IllegalArgumentException("취소된 토큰입니다.");
        }
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }

        return userRepository.findById(record.getUserOid())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public void revoke(String tokenOid) {
        SsoToken token = tokenRepository.findById(tokenOid)
                .orElseThrow(() -> new IllegalArgumentException("토큰을 찾을 수 없습니다."));
        token.setRevokedAt(LocalDateTime.now());
        log.info("[SSO] 토큰 취소 - jti={}", token.getJti());
    }

    /**
     * 로그인 체크: 현재 Access Token 검증 후 새 Access Token 재발급 (유효시간 연장)
     */
    @Transactional
    public ExtendedToken extendAccessToken(String rawToken, SsoClient client, int extendSec, String ip) {
        Claims claims;
        try {
            claims = jwtTokenService.parse(rawToken);
        } catch (JwtException e) {
            throw new IllegalArgumentException("유효하지 않은 Access Token: " + e.getMessage());
        }

        String jti = claims.getId();
        SsoToken stored = tokenRepository.findByJti(jti)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 토큰입니다."));

        if (!"ACCESS".equals(stored.getTokenType())) {
            throw new IllegalArgumentException("Access Token이 아닙니다.");
        }
        if (stored.getRevokedAt() != null) {
            throw new IllegalArgumentException("취소된 토큰입니다.");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }
        if (!stored.getClientOid().equals(client.getClientOid())) {
            throw new IllegalArgumentException("client_id가 일치하지 않습니다.");
        }

        SysUser user = userRepository.findById(stored.getUserOid())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 기존 토큰 취소 후 새 토큰 발급
        stored.setRevokedAt(LocalDateTime.now());

        String scopes = stored.getScopes() != null ? stored.getScopes() : "openid";
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenService.createAccessToken(user, client, scopes, newJti, extendSec);
        save(client, user, "ACCESS", newJti, scopes, LocalDateTime.now().plusSeconds(extendSec), ip);

        log.info("[SSO] Access Token 연장 - clientId={}, userId={}, extendSec={}", client.getClientId(), user.getUserId(), extendSec);
        return new ExtendedToken(newAccessToken, extendSec, user);
    }

    /**
     * Refresh Token으로 새 토큰 발급 (rotation: 기존 refresh 취소 후 재발급)
     */
    @Transactional
    public IssuedTokens issueFromRefresh(String rawRefreshToken, SsoClient client, String ip) {
        Claims claims;
        try {
            claims = jwtTokenService.parse(rawRefreshToken);
        } catch (JwtException e) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token: " + e.getMessage());
        }

        String jti = claims.getId();
        SsoToken stored = tokenRepository.findByJti(jti)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 토큰입니다."));

        if (!"REFRESH".equals(stored.getTokenType())) {
            throw new IllegalArgumentException("Refresh Token이 아닙니다.");
        }
        if (stored.getRevokedAt() != null) {
            throw new IllegalArgumentException("취소된 Refresh Token입니다.");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료된 Refresh Token입니다.");
        }
        if (!stored.getClientOid().equals(client.getClientOid())) {
            throw new IllegalArgumentException("client_id가 일치하지 않습니다.");
        }

        SysUser user = userRepository.findById(stored.getUserOid())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // Refresh token rotation: 기존 취소
        stored.setRevokedAt(LocalDateTime.now());

        // 새 토큰 발급
        String scopes     = stored.getScopes() != null ? stored.getScopes() : "openid";
        String accessJti  = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        String idJti      = UUID.randomUUID().toString();

        String accessToken  = jwtTokenService.createAccessToken(user, client, scopes, accessJti);
        String refreshToken = jwtTokenService.createRefreshToken(user, client, refreshJti);
        String idToken      = jwtTokenService.createIdToken(user, client, idJti);

        LocalDateTime now = LocalDateTime.now();
        save(client, user, "ACCESS",  accessJti,  scopes, now.plusSeconds(client.getAccessTokenValiditySec()),  ip);
        save(client, user, "REFRESH", refreshJti, scopes, now.plusSeconds(client.getRefreshTokenValiditySec()), ip);
        save(client, user, "ID",      idJti,      scopes, now.plusSeconds(client.getIdTokenValiditySec()),      ip);

        log.info("[SSO] Refresh 토큰 재발급 - clientId={}, userId={}", client.getClientId(), user.getUserId());
        return new IssuedTokens(accessToken, refreshToken, idToken, client.getAccessTokenValiditySec());
    }

    /**
     * 해당 client + user의 모든 활성 토큰 일괄 취소 (로그아웃)
     */
    @Transactional
    public void revokeAllByClientAndUser(String clientOid, String userOid) {
        List<SsoToken> active = tokenRepository.findActiveByClientOidAndUserOid(clientOid, userOid, LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        active.forEach(t -> t.setRevokedAt(now));
        log.info("[SSO] 전체 토큰 취소 - clientOid={}, userOid={}, count={}", clientOid, userOid, active.size());
    }

    public Page<SsoToken> findByFilter(String clientId, String userId, String tokenType,
                                       LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
        String cid  = blank(clientId)  ? null : clientId;
        String uid  = blank(userId)    ? null : userId;
        String type = blank(tokenType) ? null : tokenType;
        return tokenRepository.findByFilter(cid, uid, type, dateFrom, dateTo, pageable);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void save(SsoClient client, SysUser user, String type, String jti, String scopes,
                      LocalDateTime expiresAt, String ip) {
        SsoToken token = SsoToken.builder()
                .tokenOid(OidGenerator.generate())
                .clientOid(client.getClientOid())
                .clientId(client.getClientId())
                .userOid(user.getOid())
                .userId(user.getUserId())
                .tokenType(type)
                .jti(jti)
                .scopes(scopes)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .ipAddress(ip)
                .build();
        tokenRepository.save(token);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
