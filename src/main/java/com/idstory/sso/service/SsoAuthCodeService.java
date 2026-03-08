package com.idstory.sso.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.sso.entity.SsoAuthCode;
import com.idstory.sso.entity.SsoClient;
import com.idstory.sso.repository.SsoAuthCodeRepository;
import com.idstory.user.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SSO 인증코드(Authorization Code) 발급 및 검증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoAuthCodeService {

    private final SsoAuthCodeRepository authCodeRepository;

    /** 인증코드 발급 */
    @Transactional
    public SsoAuthCode issue(SsoClient client, SysUser user, String redirectUri, String scopes, String state, String ip) {
        String rawCode = OidGenerator.randomAlphanumeric(48);
        LocalDateTime now = LocalDateTime.now();

        SsoAuthCode code = SsoAuthCode.builder()
                .codeOid(OidGenerator.generate())
                .authCode(rawCode)
                .clientOid(client.getClientOid())
                .clientId(client.getClientId())
                .userOid(user.getOid())
                .userId(user.getUserId())
                .redirectUri(redirectUri)
                .scopes(scopes)
                .state(state)
                .issuedAt(now)
                .expiresAt(now.plusMinutes(5))
                .status("ISSUED")
                .ipAddress(ip)
                .build();

        authCodeRepository.save(code);
        log.info("[SSO] 인증코드 발급 - clientId={}, userId={}, ip={}", client.getClientId(), user.getUserId(), ip);
        return code;
    }

    /**
     * 인증코드 검증 및 USED 처리
     * @throws IllegalArgumentException 유효하지 않은 코드
     */
    @Transactional
    public SsoAuthCode consume(String authCode, String clientId, String redirectUri) {
        SsoAuthCode code = authCodeRepository.findByAuthCode(authCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증코드입니다."));

        if (!code.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("client_id가 일치하지 않습니다.");
        }
        if (!code.getStatus().equals("ISSUED")) {
            throw new IllegalArgumentException("이미 사용되었거나 만료된 인증코드입니다.");
        }
        if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
            code.setStatus("EXPIRED");
            throw new IllegalArgumentException("만료된 인증코드입니다.");
        }
        if (!code.getRedirectUri().equals(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri가 일치하지 않습니다.");
        }

        code.setStatus("USED");
        code.setUsedAt(LocalDateTime.now());
        log.info("[SSO] 인증코드 사용 - clientId={}, code={}...", clientId, authCode.substring(0, 8));
        return code;
    }

    public Page<SsoAuthCode> findByFilter(String clientId, String userId, String status,
                                          LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
        String cid = (clientId != null && !clientId.isBlank()) ? clientId : null;
        String uid = (userId   != null && !userId.isBlank())   ? userId   : null;
        String st  = (status   != null && !status.isBlank())   ? status   : null;
        return authCodeRepository.findByFilter(cid, uid, st, dateFrom, dateTo, pageable);
    }
}
