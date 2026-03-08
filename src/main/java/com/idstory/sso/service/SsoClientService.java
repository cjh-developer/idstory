package com.idstory.sso.service;

import com.idstory.client.repository.ClientRepository;
import com.idstory.common.util.OidGenerator;
import com.idstory.sso.dto.SsoClientCreateDto;
import com.idstory.sso.dto.SsoClientUpdateDto;
import com.idstory.sso.entity.SsoClient;
import com.idstory.sso.repository.SsoClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SSO 클라이언트 관리 서비스
 * - Client Secret: 생성 시 1회만 반환, DB에는 SHA-256 HEX만 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoClientService {

    private final SsoClientRepository ssoClientRepository;
    private final ClientRepository    clientRepository;

    public List<SsoClient> findAll() {
        return ssoClientRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<SsoClient> findByClientOid(String clientOid) {
        return ssoClientRepository.findByClientOid(clientOid);
    }

    public SsoClient findBySsoClientOid(String ssoClientOid) {
        return ssoClientRepository.findById(ssoClientOid)
                .orElseThrow(() -> new IllegalArgumentException("SSO 클라이언트를 찾을 수 없습니다."));
    }

    public SsoClient findByClientId(String clientId) {
        return ssoClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("client_id를 찾을 수 없습니다: " + clientId));
    }

    /**
     * SSO 클라이언트 등록
     * @return rawSecret (1회만 노출용, DB 미저장)
     */
    @Transactional
    public String create(SsoClientCreateDto dto, String performedBy) {
        // ids_iam_client 존재 확인 (소프트 삭제되지 않은 것)
        boolean clientExists = clientRepository.findById(dto.getClientOid())
                .filter(c -> c.getDeletedAt() == null)
                .isPresent();
        if (!clientExists) {
            throw new IllegalArgumentException("존재하지 않는 클라이언트입니다.");
        }
        // SSO 중복 등록 확인
        if (ssoClientRepository.existsByClientOid(dto.getClientOid())) {
            throw new IllegalArgumentException("이미 SSO가 등록된 클라이언트입니다.");
        }
        // client_id 중복 확인
        if (ssoClientRepository.existsByClientId(dto.getClientId())) {
            throw new IllegalArgumentException("이미 사용 중인 Client ID입니다.");
        }

        String rawSecret = OidGenerator.randomAlphanumeric(32);
        String encSecret = sha256Hex(rawSecret);

        SsoClient client = SsoClient.builder()
                .ssoClientOid(OidGenerator.generate())
                .clientOid(dto.getClientOid())
                .clientId(dto.getClientId())
                .encClientSecret(encSecret)
                .redirectUris(dto.getRedirectUris())
                .scopes(dto.getScopes() != null ? dto.getScopes() : "openid profile email")
                .grantTypes(dto.getGrantTypes() != null ? dto.getGrantTypes() : "authorization_code")
                .accessTokenValiditySec(dto.getAccessTokenValiditySec() > 0 ? dto.getAccessTokenValiditySec() : 3600)
                .refreshTokenValiditySec(dto.getRefreshTokenValiditySec() > 0 ? dto.getRefreshTokenValiditySec() : 86400)
                .idTokenValiditySec(dto.getIdTokenValiditySec() > 0 ? dto.getIdTokenValiditySec() : 3600)
                .useYn("Y")
                .createdBy(performedBy)
                .updatedBy(performedBy)
                .build();

        ssoClientRepository.save(client);
        log.info("[SSO] 클라이언트 등록 - clientId={}, by={}", dto.getClientId(), performedBy);
        return rawSecret;
    }

    @Transactional
    public void update(String ssoClientOid, SsoClientUpdateDto dto, String performedBy) {
        SsoClient client = findBySsoClientOid(ssoClientOid);
        client.setRedirectUris(dto.getRedirectUris());
        client.setScopes(dto.getScopes());
        client.setGrantTypes(dto.getGrantTypes());
        client.setAccessTokenValiditySec(dto.getAccessTokenValiditySec());
        client.setRefreshTokenValiditySec(dto.getRefreshTokenValiditySec());
        client.setIdTokenValiditySec(dto.getIdTokenValiditySec());
        client.setUseYn(dto.getUseYn());
        client.setUpdatedBy(performedBy);
        log.info("[SSO] 클라이언트 수정 - ssoClientOid={}, by={}", ssoClientOid, performedBy);
    }

    @Transactional
    public void delete(String ssoClientOid, String performedBy) {
        SsoClient client = findBySsoClientOid(ssoClientOid);
        ssoClientRepository.delete(client);
        log.info("[SSO] 클라이언트 삭제 - ssoClientOid={}, by={}", ssoClientOid, performedBy);
    }

    /**
     * 클라이언트 OID 기반 SSO 클라이언트 생성 (ClientController에서 사용)
     * @return rawSecret (1회 노출)
     */
    @Transactional
    public String createFromClient(String clientOid, String clientId, String authUri,
                                   String authResult, String noUseSso,
                                   String redirectUris, String scopes, String performedBy) {
        if (ssoClientRepository.existsByClientOid(clientOid)) {
            throw new IllegalArgumentException("이미 SSO가 등록된 클라이언트입니다.");
        }
        if (ssoClientRepository.existsByClientId(clientId)) {
            throw new IllegalArgumentException("이미 사용 중인 Client ID입니다.");
        }

        String rawSecret = OidGenerator.randomAlphanumeric(32);
        String encSecret = sha256Hex(rawSecret);

        SsoClient sc = SsoClient.builder()
                .ssoClientOid(OidGenerator.generate())
                .clientOid(clientOid)
                .clientId(clientId)
                .encClientSecret(encSecret)
                .redirectUris(redirectUris != null ? redirectUris : "")
                .scopes(scopes != null ? scopes : "openid profile email")
                .grantTypes("authorization_code")
                .accessTokenValiditySec(3600)
                .refreshTokenValiditySec(86400)
                .idTokenValiditySec(3600)
                .useYn("Y")
                .authUri(authUri)
                .authResult(authResult)
                .noUseSso(noUseSso)
                .createdBy(performedBy)
                .updatedBy(performedBy)
                .build();

        ssoClientRepository.save(sc);
        log.info("[SSO] 클라이언트 등록(client통합) - clientId={}, by={}", clientId, performedBy);
        return rawSecret;
    }

    /**
     * SSO 설정 수정 (clientOid 기준)
     */
    @Transactional
    public void updateByClientOid(String clientOid, SsoClientUpdateDto dto, String performedBy) {
        SsoClient sc = ssoClientRepository.findByClientOid(clientOid)
                .orElseThrow(() -> new IllegalArgumentException("SSO 설정이 없습니다."));
        sc.setRedirectUris(dto.getRedirectUris());
        sc.setScopes(dto.getScopes());
        sc.setGrantTypes(dto.getGrantTypes());
        sc.setAccessTokenValiditySec(dto.getAccessTokenValiditySec());
        sc.setRefreshTokenValiditySec(dto.getRefreshTokenValiditySec());
        sc.setIdTokenValiditySec(dto.getIdTokenValiditySec());
        sc.setUseYn(dto.getUseYn());
        sc.setAuthUri(dto.getAuthUri());
        sc.setAuthResult(dto.getAuthResult());
        sc.setNoUseSso(dto.getNoUseSso());
        sc.setUpdatedBy(performedBy);
        log.info("[SSO] 설정 수정(client통합) - clientOid={}, by={}", clientOid, performedBy);
    }

    /**
     * Secret 재발급 (clientOid 기준)
     * @return 새 rawSecret (1회 노출)
     */
    @Transactional
    public String regenerateSecretByClientOid(String clientOid, String performedBy) {
        SsoClient sc = ssoClientRepository.findByClientOid(clientOid)
                .orElseThrow(() -> new IllegalArgumentException("SSO 설정이 없습니다."));
        String rawSecret = OidGenerator.randomAlphanumeric(32);
        sc.setEncClientSecret(sha256Hex(rawSecret));
        sc.setUpdatedBy(performedBy);
        log.info("[SSO] Secret 재발급(client통합) - clientOid={}, by={}", clientOid, performedBy);
        return rawSecret;
    }

    /** SSO 상세 Map (authUri, authResult, noUseSso 포함) */
    public Map<String, Object> toDetailMap(SsoClient sc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ssoClientOid",            sc.getSsoClientOid());
        m.put("clientOid",               sc.getClientOid());
        m.put("clientId",                sc.getClientId());
        m.put("redirectUris",            sc.getRedirectUris());
        m.put("scopes",                  sc.getScopes());
        m.put("grantTypes",              sc.getGrantTypes());
        m.put("accessTokenValiditySec",  sc.getAccessTokenValiditySec());
        m.put("refreshTokenValiditySec", sc.getRefreshTokenValiditySec());
        m.put("idTokenValiditySec",      sc.getIdTokenValiditySec());
        m.put("useYn",                   sc.getUseYn());
        m.put("authUri",                 sc.getAuthUri());
        m.put("authResult",              sc.getAuthResult());
        m.put("noUseSso",               sc.getNoUseSso());
        return m;
    }

    /**
     * Secret 재발급
     * @return 새 rawSecret (1회 노출)
     */
    @Transactional
    public String regenerateSecret(String ssoClientOid, String performedBy) {
        SsoClient client = findBySsoClientOid(ssoClientOid);
        String rawSecret = OidGenerator.randomAlphanumeric(32);
        client.setEncClientSecret(sha256Hex(rawSecret));
        client.setUpdatedBy(performedBy);
        log.info("[SSO] Secret 재발급 - ssoClientOid={}, by={}", ssoClientOid, performedBy);
        return rawSecret;
    }

    /**
     * client_id + rawSecret 검증
     * @return 검증된 SsoClient
     */
    public SsoClient verifySecret(String clientId, String rawSecret) {
        SsoClient client = ssoClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 client_id: " + clientId));
        if (!"Y".equals(client.getUseYn())) {
            throw new IllegalArgumentException("비활성 SSO 클라이언트입니다.");
        }
        if (!sha256Hex(rawSecret).equals(client.getEncClientSecret())) {
            throw new IllegalArgumentException("Client Secret이 올바르지 않습니다.");
        }
        return client;
    }

    /** SHA-256 해시 (HEX 소문자 64자) */
    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 해시 실패", e);
        }
    }

    /** 관리 페이지 목록용 Map */
    public Map<String, Object> toMap(SsoClient c) {
        return Map.of(
                "ssoClientOid",            c.getSsoClientOid(),
                "clientOid",               c.getClientOid(),
                "clientId",                c.getClientId(),
                "redirectUris",            c.getRedirectUris(),
                "scopes",                  c.getScopes(),
                "grantTypes",              c.getGrantTypes(),
                "accessTokenValiditySec",  c.getAccessTokenValiditySec(),
                "refreshTokenValiditySec", c.getRefreshTokenValiditySec(),
                "idTokenValiditySec",      c.getIdTokenValiditySec(),
                "useYn",                   c.getUseYn()
        );
    }
}
