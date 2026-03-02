package com.idstory.userorgmap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자-조직 매핑 엔티티 (주소속 / 겸직)
 * 매핑 테이블: ids_iam_user_org_map
 */
@Entity
@Table(name = "ids_iam_user_org_map")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrgMap {

    @Id
    @Column(name = "map_oid", length = 18)
    private String mapOid;

    @Column(name = "user_oid", nullable = false, length = 18)
    private String userOid;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "dept_oid", length = 18)
    private String deptOid;

    @Column(name = "dept_name", length = 100)
    private String deptName;

    @Column(name = "position_oid", length = 18)
    private String positionOid;

    @Column(name = "position_name", length = 100)
    private String positionName;

    @Column(name = "grade_oid", length = 18)
    private String gradeOid;

    @Column(name = "grade_name", length = 100)
    private String gradeName;

    @Column(name = "comp_role_oid", length = 18)
    private String compRoleOid;

    @Column(name = "comp_role_name", length = 100)
    private String compRoleName;

    /** 주소속 여부 Y=주소속, N=겸직 */
    @Column(name = "is_primary", nullable = false, length = 1)
    @Builder.Default
    private String isPrimary = "Y";

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
