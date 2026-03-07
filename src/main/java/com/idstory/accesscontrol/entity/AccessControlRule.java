package com.idstory.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IP/MAC 접근 제어 규칙 엔티티
 * 매핑 테이블: ids_iam_access_control
 */
@Entity
@Table(name = "ids_iam_access_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessControlRule {

    /** 규칙 OID (PK) */
    @Id
    @Column(name = "rule_oid", length = 18)
    private String ruleOid;

    /** 제어 유형 (IP | MAC) */
    @Column(name = "control_type", nullable = false, length = 10)
    private String controlType;

    /** IP/CIDR 또는 MAC 주소 */
    @Column(name = "rule_value", nullable = false, length = 50)
    private String ruleValue;

    /** IP 버전 (IPV4 | IPV6, MAC이면 NULL) */
    @Column(name = "ip_version", length = 4)
    private String ipVersion;

    /** 설명 */
    @Column(name = "description", length = 200)
    private String description;

    /** 규칙 활성화 여부 Y|N */
    @Column(name = "use_yn", nullable = false, length = 1)
    @Builder.Default
    private String useYn = "Y";

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
