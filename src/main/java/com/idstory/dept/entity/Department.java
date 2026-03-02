package com.idstory.dept.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 부서 엔티티
 * 매핑 테이블: ids_iam_dept
 */
@Entity
@Table(name = "ids_iam_dept")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    /** 부서 OID (PK, ids_+14자) */
    @Id
    @Column(name = "dept_oid", length = 18, nullable = false)
    private String deptOid;

    /** 부서코드 (UNIQUE, ids_iam_user.dept_code FK 참조 대상) */
    @Column(name = "dept_code", length = 20, nullable = false, unique = true)
    private String deptCode;

    /** 부서명 */
    @Column(name = "dept_name", nullable = false, length = 100)
    private String deptName;

    /** 상위 부서 OID (NULL=최상위) */
    @Column(name = "parent_dept_oid", length = 18)
    private String parentDeptOid;

    /** 정렬 순서 */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /** 사용여부 Y|N */
    @Column(name = "use_yn", nullable = false, length = 1)
    @Builder.Default
    private String useYn = "Y";

    /** 부서 유형 */
    @Column(name = "dept_type", length = 20)
    private String deptType;

    /** 부서 전화번호 */
    @Column(name = "dept_tel", length = 20)
    private String deptTel;

    /** 부서 팩스번호 */
    @Column(name = "dept_fax", length = 20)
    private String deptFax;

    /** 부서 주소 */
    @Column(name = "dept_address", length = 200)
    private String deptAddress;

    /** 생성 일시 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 생성자 */
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    /** 수정 일시 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 수정자 */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /** 삭제 일시 (소프트 삭제, NULL=미삭제) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 삭제자 */
    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    @Column(name = "reserve_field_1", length = 255)
    private String reserveField1;

    @Column(name = "reserve_field_2", length = 255)
    private String reserveField2;

    @Column(name = "reserve_field_3", length = 255)
    private String reserveField3;

    @Column(name = "reserve_field_4", length = 255)
    private String reserveField4;

    @Column(name = "reserve_field_5", length = 255)
    private String reserveField5;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
