package com.idstory.sso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SSO 발행 토큰 이력 엔티티
 * - token_type: ACCESS | REFRESH | ID
 * - jti: JWT ID (UUID), unique
 */
@Entity
@Table(name = "ids_iam_sso_token")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SsoToken {

    @Id
    @Column(name = "token_oid", length = 18)
    private String tokenOid;

    @Column(name = "client_oid", length = 18, nullable = false)
    private String clientOid;

    @Column(name = "client_id", length = 100, nullable = false)
    private String clientId;

    @Column(name = "user_oid", length = 18, nullable = false)
    private String userOid;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /** ACCESS | REFRESH | ID */
    @Column(name = "token_type", length = 10, nullable = false)
    private String tokenType;

    @Column(name = "jti", length = 100, nullable = false, unique = true)
    private String jti;

    @Column(name = "scopes", length = 200)
    private String scopes;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) this.issuedAt = LocalDateTime.now();
    }
}
