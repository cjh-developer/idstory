package com.idstory.admin.entity;

import com.idstory.user.entity.SysUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 관리자 추가 정보 엔티티
 *
 * <p>sys_users 에서 role=ADMIN 인 사용자의 추가 메타정보를 저장합니다.
 * 관리자 등록(승격) 시 sys_admins 레코드가 생성되고,
 * 관리자 해제(강등) 시 레코드가 삭제됩니다.</p>
 */
@Entity
@Table(name = "ids_iam_admin")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysAdmin {

    @Id
    @Column(name = "admin_oid", length = 18, nullable = false)
    private String adminOid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_oid", nullable = false)
    private SysUser user;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "granted_by", length = 50)
    private String grantedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
