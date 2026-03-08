package com.idstory.sso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SSO RSA 서명 키 쌍 엔티티
 * - 서버 기동 시 없으면 자동 생성, DB에 영구 보존
 */
@Entity
@Table(name = "ids_iam_sso_key_pair")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SsoKeyPair {

    @Id
    @Column(name = "key_id", length = 36)
    private String keyId;

    @Column(name = "private_key_pem", columnDefinition = "TEXT", nullable = false)
    private String privateKeyPem;

    @Column(name = "public_key_pem", columnDefinition = "TEXT", nullable = false)
    private String publicKeyPem;

    @Column(name = "active_yn", length = 1, nullable = false)
    private String activeYn = "Y";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
