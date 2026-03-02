package com.idstory.policy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 비밀번호 정책 엔티티 (key-value 구조)
 * 매핑 테이블: ids_iam_pwd_policy
 */
@Entity
@Table(name = "ids_iam_pwd_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordPolicy {

    /** 정책 키 (PK) */
    @Id
    @Column(name = "policy_key", length = 50)
    private String policyKey;

    /** 정책 값 */
    @Column(name = "policy_value", nullable = false, length = 200)
    private String policyValue;

    /** 정책 설명 */
    @Column(name = "description", length = 200)
    private String description;

    /** 수정일 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 수정자 */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
