package com.idstory.orghistory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 조직 이력 엔티티 (직위·직급 통합 이력)
 * 매핑 테이블: ids_iam_org_history
 */
@Entity
@Table(name = "ids_iam_org_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgHistory {

    @Id
    @Column(name = "history_oid", length = 18, nullable = false)
    private String historyOid;

    /** 대상 구분: POSITION | GRADE */
    @Column(name = "target_type", length = 20, nullable = false)
    private String targetType;

    @Column(name = "target_oid", length = 18, nullable = false)
    private String targetOid;

    /** 처리 구분: CREATE | UPDATE | DELETE */
    @Column(name = "action_type", length = 20, nullable = false)
    private String actionType;

    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @Column(name = "action_by", length = 50)
    private String actionBy;
}
