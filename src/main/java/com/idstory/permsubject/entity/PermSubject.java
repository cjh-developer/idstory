package com.idstory.permsubject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 권한 대상 매핑 엔티티
 * 매핑 테이블: ids_iam_perm_subject
 * subject_type: DEPT | USER | GRADE | POSITION | EXCEPTION
 */
@Entity
@Table(name = "ids_iam_perm_subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermSubject {

    @Id
    @Column(name = "perm_subject_oid", length = 18, nullable = false)
    private String permSubjectOid;

    @Column(name = "perm_oid", length = 18, nullable = false)
    private String permOid;

    /** 대상 유형: DEPT | USER | GRADE | POSITION | EXCEPTION */
    @Column(name = "subject_type", length = 20, nullable = false)
    private String subjectType;

    /** 대상 OID (dept_oid | user oid | grade_oid | position_oid) */
    @Column(name = "subject_oid", length = 18, nullable = false)
    private String subjectOid;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
