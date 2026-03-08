package com.idstory.sso.service;

import com.idstory.sso.entity.SsoClient;
import com.idstory.user.entity.SysUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * OIDC JWT 토큰 생성/파싱 서비스 (RS256)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final SsoKeyPairService keyPairService;

    @Value("${sso.issuer:http://localhost:9091}")
    private String issuer;

    /** Access Token (JWT) 생성 */
    public String createAccessToken(SysUser user, SsoClient client, String scopes, String jti) {
        return createAccessToken(user, client, scopes, jti, client.getAccessTokenValiditySec());
    }

    /** Access Token (JWT) 생성 — 커스텀 유효시간(초) */
    public String createAccessToken(SysUser user, SsoClient client, String scopes, String jti, int validitySec) {
        Date now    = new Date();
        Date expiry = toDate(LocalDateTime.now().plusSeconds(validitySec));

        return Jwts.builder()
                .header().keyId(keyPairService.getKeyId()).and()
                .issuer(issuer)
                .subject(user.getOid())
                .audience().add(client.getClientId()).and()
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .claim("scope", scopes)
                .claim("client_id", client.getClientId())
                .signWith(keyPairService.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /** ID Token (JWT) 생성 */
    public String createIdToken(SysUser user, SsoClient client, String jti) {
        Date now    = new Date();
        Date expiry = toDate(LocalDateTime.now().plusSeconds(client.getIdTokenValiditySec()));

        return Jwts.builder()
                .header().keyId(keyPairService.getKeyId()).and()
                .issuer(issuer)
                .subject(user.getOid())
                .audience().add(client.getClientId()).and()
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .claim("name",               user.getName())
                .claim("preferred_username", user.getUserId())
                .claim("email",              user.getEmail())
                .signWith(keyPairService.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /** Refresh Token (JWT) 생성 */
    public String createRefreshToken(SysUser user, SsoClient client, String jti) {
        Date now    = new Date();
        Date expiry = toDate(LocalDateTime.now().plusSeconds(client.getRefreshTokenValiditySec()));

        return Jwts.builder()
                .header().keyId(keyPairService.getKeyId()).and()
                .issuer(issuer)
                .subject(user.getOid())
                .audience().add(client.getClientId()).and()
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .claim("token_use", "refresh")
                .signWith(keyPairService.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * 토큰 파싱 및 Claims 반환
     * @throws JwtException 서명 오류, 만료 등
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(keyPairService.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** LocalDateTime → java.util.Date */
    private Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
