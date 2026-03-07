package com.idstory.roleuser.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 역할-대상 매핑 엔티티 (부서/직위/직급/예외)
 * 매핑 테이블: ids_iam_role_subject
 */
@Entity
@Table(name = "ids_iam_role_subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleSubject {

    @Id
    @Column(name = "role_subject_oid", length = 18, nullable = false)
    private String roleSubjectOid;

    @Column(name = "role_oid", length = 18, nullable = false)
    private String roleOid;

    /** 대상 유형: DEPT | POSITION | GRADE | EXCEPTION */
    @Column(name = "subject_type", length = 20, nullable = false)
    private String subjectType;

    /** 대상 OID (dept_oid / position_oid / grade_oid / user_oid) */
    @Column(name = "subject_oid", length = 18, nullable = false)
    private String subjectOid;

    /** 하위 부서 포함 여부 Y|N (DEPT 타입 전용) */
    @Column(name = "include_children", length = 1, nullable = false)
    @Builder.Default
    private String includeChildren = "N";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
