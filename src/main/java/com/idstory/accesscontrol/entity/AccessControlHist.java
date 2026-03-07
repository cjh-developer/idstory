package com.idstory.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 접근 제어 차단 이력 엔티티
 * 매핑 테이블: ids_iam_access_control_hist
 */
@Entity
@Table(name = "ids_iam_access_control_hist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessControlHist {

    /** 이력 OID (PK) */
    @Id
    @Column(name = "hist_oid", length = 18)
    private String histOid;

    /** 차단 유형 (IP | MAC) */
    @Column(name = "control_type", nullable = false, length = 10)
    private String controlType;

    /** 차단된 접속값 (IP 주소 또는 MAC) */
    @Column(name = "request_val", nullable = false, length = 100)
    private String requestVal;

    /** 요청 URI */
    @Column(name = "request_uri", length = 255)
    private String requestUri;

    /** 차단 일시 */
    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    @PrePersist
    protected void onCreate() {
        if (this.blockedAt == null) {
            this.blockedAt = LocalDateTime.now();
        }
    }
}
