package com.idstory.sso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SSO 접근 이력 엔티티
 * - action: AUTHORIZE | TOKEN | USERINFO | LOGOUT | DISCOVERY | JWKS
 * - status: SUCCESS | FAIL | DENIED
 */
@Entity
@Table(name = "ids_iam_sso_access_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SsoAccessLog {

    @Id
    @Column(name = "log_oid", length = 18)
    private String logOid;

    @Column(name = "client_oid", length = 18)
    private String clientOid;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "user_oid", length = 18)
    private String userOid;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "status", length = 10, nullable = false)
    private String status;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "accessed_at", nullable = false)
    private LocalDateTime accessedAt;

    @PrePersist
    protected void onCreate() {
        if (this.accessedAt == null) this.accessedAt = LocalDateTime.now();
    }
}
