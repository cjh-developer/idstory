package com.idstory.history.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 로그인 이력 엔티티
 *
 * <p>action_type 값:</p>
 * <ul>
 *   <li>LOGIN_SUCCESS — 로그인 성공</li>
 *   <li>LOGIN_FAIL    — 로그인 실패</li>
 *   <li>LOGOUT        — 로그아웃</li>
 * </ul>
 *
 * <p>user_oid 는 존재하지 않는 계정의 시도도 기록하기 위해 FK 없이 저장합니다.</p>
 */
@Entity
@Table(name = "ids_iam_login_hist")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistory {

    @Id
    @Column(name = "hist_oid", length = 18, nullable = false)
    private String histOid;

    @Column(name = "user_oid", length = 18)
    private String userOid;             // FK 없음 — 존재하지 않는 계정도 기록

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "action_type", length = 20, nullable = false)
    private String actionType;          // LOGIN_SUCCESS | LOGIN_FAIL | LOGOUT

    @Column(name = "fail_reason", length = 100)
    private String failReason;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
