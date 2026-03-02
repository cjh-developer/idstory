package com.idstory.depthead.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 부서장 매핑 엔티티
 * 매핑 테이블: ids_iam_dept_head
 */
@Entity
@Table(name = "ids_iam_dept_head")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeptHead {

    @Id
    @Column(name = "head_oid", length = 18)
    private String headOid;

    @Column(name = "dept_oid", nullable = false, length = 18, unique = true)
    private String deptOid;

    @Column(name = "dept_name", length = 100)
    private String deptName;

    @Column(name = "user_oid", nullable = false, length = 18)
    private String userOid;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

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
