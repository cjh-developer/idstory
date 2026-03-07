package com.idstory.policy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 정책 변경 이력 엔티티
 * 매핑 테이블: ids_iam_policy_hist
 */
@Entity
@Table(name = "ids_iam_policy_hist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemPolicyHistory {

    /** 이력 OID */
    @Id
    @Column(name = "hist_oid", length = 18)
    private String histOid;

    /** 정책 그룹 */
    @Column(name = "policy_group", nullable = false, length = 30)
    private String policyGroup;

    /** 정책 키 */
    @Column(name = "policy_key", nullable = false, length = 60)
    private String policyKey;

    /** 변경 전 값 */
    @Column(name = "old_value", length = 500)
    private String oldValue;

    /** 변경 후 값 */
    @Column(name = "new_value", length = 500)
    private String newValue;

    /** 변경 일시 */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /** 변경자 */
    @Column(name = "changed_by", length = 50)
    private String changedBy;

    @PrePersist
    protected void onCreate() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }
}
