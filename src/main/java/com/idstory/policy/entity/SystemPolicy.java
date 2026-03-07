package com.idstory.policy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 통합 정책 엔티티
 * 매핑 테이블: ids_iam_policy
 */
@Entity
@Table(name = "ids_iam_policy")
@IdClass(SystemPolicyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemPolicy {

    /** 정책 그룹 (ADMIN_POLICY|USER_POLICY|PASSWORD_POLICY|LOGIN_POLICY|ACCOUNT_POLICY|AUDIT_POLICY|SYSTEM_POLICY) */
    @Id
    @Column(name = "policy_group", length = 30)
    private String policyGroup;

    /** 정책 키 */
    @Id
    @Column(name = "policy_key", length = 60)
    private String policyKey;

    /** 정책 값 */
    @Column(name = "policy_value", nullable = false, length = 500)
    private String policyValue;

    /** 값 유형 (STRING|INTEGER|BOOLEAN|ENUM|MULTI_STRING) */
    @Column(name = "value_type", nullable = false, length = 20)
    @Builder.Default
    private String valueType = "STRING";

    /** 정책 설명 */
    @Column(name = "description", length = 200)
    private String description;

    /** 수정일 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 수정자 */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
