package com.idstory.roleuser.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 역할-사용자 매핑 엔티티
 * 매핑 테이블: ids_iam_role_user
 */
@Entity
@Table(name = "ids_iam_role_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleUser {

    @Id
    @Column(name = "role_user_oid", length = 18, nullable = false)
    private String roleUserOid;

    @Column(name = "role_oid", length = 18, nullable = false)
    private String roleOid;

    @Column(name = "user_oid", length = 18, nullable = false)
    private String userOid;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
