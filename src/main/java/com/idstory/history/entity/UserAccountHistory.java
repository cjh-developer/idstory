package com.idstory.history.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 계정 이력 엔티티
 * 매핑 테이블: ids_iam_user_acct_hist
 */
@Entity
@Table(name = "ids_iam_user_acct_hist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountHistory {

    /** 이력 OID */
    @Id
    @Column(name = "hist_oid", length = 18)
    private String histOid;

    /** 대상 사용자 OID */
    @Column(name = "target_user_oid", nullable = false, length = 18)
    private String targetUserOid;

    /** 대상 사용자 계정 (비정규화) */
    @Column(name = "target_username", length = 50)
    private String targetUsername;

    /** 처리 유형 (CREATE|UPDATE|DELETE|LOCK|UNLOCK|RESET_PWD) */
    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    /** 변경 상세 */
    @Column(name = "action_detail", length = 500)
    private String actionDetail;

    /** 처리자 username */
    @Column(name = "performed_by", length = 50)
    private String performedBy;

    /** 처리 일시 */
    @Column(name = "performed_at", nullable = false)
    @Builder.Default
    private LocalDateTime performedAt = LocalDateTime.now();

    /** IP 주소 */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
