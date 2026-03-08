package com.idstory.sso.service;

import com.idstory.sso.entity.SsoKeyPair;
import com.idstory.sso.repository.SsoKeyPairRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * SSO RSA 서명 키 관리 서비스
 * - 서버 기동 시 DB에서 활성 키 로드, 없으면 RSA-2048 생성 후 DB 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoKeyPairService {

    private final SsoKeyPairRepository keyPairRepository;

    private RSAPrivateKey privateKey;
    private RSAPublicKey  publicKey;
    private String        keyId;

    @PostConstruct
    @Transactional
    public void init() {
        keyPairRepository.findFirstByActiveYnOrderByCreatedAtAsc("Y")
                .ifPresentOrElse(this::loadFromEntity, this::generateAndSave);
        log.info("[SSO] RSA 서명 키 로드 완료 - keyId={}", keyId);
    }

    public RSAPrivateKey getPrivateKey() { return privateKey; }
    public RSAPublicKey  getPublicKey()  { return publicKey; }
    public String        getKeyId()      { return keyId; }

    // ── private ──────────────────────────────────────────────────────────────

    private void loadFromEntity(SsoKeyPair entity) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] privBytes = Base64.getDecoder().decode(stripPem(entity.getPrivateKeyPem()));
            byte[] pubBytes  = Base64.getDecoder().decode(stripPem(entity.getPublicKeyPem()));
            privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
            publicKey  = (RSAPublicKey)  kf.generatePublic(new X509EncodedKeySpec(pubBytes));
            keyId = entity.getKeyId();
        } catch (Exception e) {
            throw new IllegalStateException("SSO RSA 키 로드 실패", e);
        }
    }

    private void generateAndSave() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();
            privateKey = (RSAPrivateKey) pair.getPrivate();
            publicKey  = (RSAPublicKey)  pair.getPublic();
            keyId = UUID.randomUUID().toString();

            String privPem = toPem(privateKey.getEncoded(), "PRIVATE KEY");
            String pubPem  = toPem(publicKey.getEncoded(),  "PUBLIC KEY");

            SsoKeyPair entity = SsoKeyPair.builder()
                    .keyId(keyId)
                    .privateKeyPem(privPem)
                    .publicKeyPem(pubPem)
                    .activeYn("Y")
                    .build();
            keyPairRepository.save(entity);
            log.info("[SSO] RSA 서명 키 신규 생성 - keyId={}", keyId);
        } catch (Exception e) {
            throw new IllegalStateException("SSO RSA 키 생성 실패", e);
        }
    }

    private String toPem(byte[] encoded, String type) {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----";
    }

    private String stripPem(String pem) {
        return pem.replaceAll("-----.*-----", "").replaceAll("\\s", "");
    }
}
