package com.idstory.permrole.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 권한-역할 매핑 엔티티
 * 매핑 테이블: ids_iam_perm_role
 */
@Entity
@Table(name = "ids_iam_perm_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermRole {

    @Id
    @Column(name = "perm_role_oid", length = 18, nullable = false)
    private String permRoleOid;

    @Column(name = "perm_oid", length = 18, nullable = false)
    private String permOid;

    @Column(name = "role_oid", length = 18, nullable = false)
    private String roleOid;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
