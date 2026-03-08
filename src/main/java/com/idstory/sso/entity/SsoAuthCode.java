package com.idstory.sso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SSO 인증코드(Authorization Code) 발행 이력 엔티티
 * - status: ISSUED → USED | EXPIRED
 */
@Entity
@Table(name = "ids_iam_sso_auth_code")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SsoAuthCode {

    @Id
    @Column(name = "code_oid", length = 18)
    private String codeOid;

    @Column(name = "auth_code", length = 128, nullable = false, unique = true)
    private String authCode;

    @Column(name = "client_oid", length = 18, nullable = false)
    private String clientOid;

    @Column(name = "client_id", length = 100, nullable = false)
    private String clientId;

    @Column(name = "user_oid", length = 18, nullable = false)
    private String userOid;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "redirect_uri", length = 1000, nullable = false)
    private String redirectUri;

    @Column(name = "scopes", length = 200)
    private String scopes;

    @Column(name = "state", length = 500)
    private String state;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** ISSUED | USED | EXPIRED */
    @Column(name = "status", length = 10, nullable = false)
    private String status = "ISSUED";

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) this.issuedAt = LocalDateTime.now();
    }
}
