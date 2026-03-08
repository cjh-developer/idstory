package com.idstory.sso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SSO OIDC 클라이언트 설정 엔티티
 * - ids_iam_client와 1:1 관계 (client_oid FK)
 * - enc_client_secret: SHA-256 HEX (원문 미저장)
 */
@Entity
@Table(name = "ids_iam_sso_client")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SsoClient {

    @Id
    @Column(name = "sso_client_oid", length = 18)
    private String ssoClientOid;

    @Column(name = "client_oid", length = 18, nullable = false, unique = true)
    private String clientOid;

    @Column(name = "client_id", length = 100, nullable = false, unique = true)
    private String clientId;

    @Column(name = "enc_client_secret", length = 64, nullable = false)
    private String encClientSecret;

    @Column(name = "redirect_uris", columnDefinition = "TEXT", nullable = false)
    private String redirectUris;

    @Column(name = "scopes", length = 500, nullable = false)
    private String scopes;

    @Column(name = "grant_types", length = 200, nullable = false)
    private String grantTypes;

    @Column(name = "access_token_validity_sec", nullable = false)
    private int accessTokenValiditySec = 3600;

    @Column(name = "refresh_token_validity_sec", nullable = false)
    private int refreshTokenValiditySec = 86400;

    @Column(name = "id_token_validity_sec", nullable = false)
    private int idTokenValiditySec = 3600;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    @Column(name = "auth_uri", length = 500)
    private String authUri;

    @Column(name = "auth_result", length = 500)
    private String authResult;

    @Column(name = "no_use_sso", columnDefinition = "TEXT")
    private String noUseSso;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
