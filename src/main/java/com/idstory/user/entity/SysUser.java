package com.idstory.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 시스템 사용자 엔티티
 * 매핑 테이블: ids_iam_user
 */
@Entity
@Table(name = "ids_iam_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUser {

    /** 사용자 OID (ids_+14자, 총 18자) — PK */
    @Id
    @Column(name = "oid", length = 18)
    private String oid;

    /** 로그인 계정 (유니크) */
    @Column(name = "user_id", unique = true, nullable = false, length = 50)
    private String userId;

    /** 이름 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 비밀번호 (순수 해시) */
    @Column(name = "password", nullable = false, length = 350)
    private String password;

    /** 비밀번호 Salt (XML 설정값, password-salt-enabled=true 시 저장) */
    @Column(name = "password_salt", length = 20)
    private String passwordSalt;

    /** 휴대번호 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 이메일 (유니크) */
    @Column(name = "email", unique = true, length = 100)
    private String email;

    /** 부서코드 */
    @Column(name = "dept_code", length = 20)
    private String deptCode;

    /** 역할 (USER | ADMIN) */
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "USER";

    /** 사용여부 Y|N */
    @Column(name = "use_yn", nullable = false, length = 1)
    @Builder.Default
    private String useYn = "Y";

    /** 상태 (ACTIVE|SLEEPER|OUT) */
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private String status = "ACTIVE";

    /** 잠금여부 Y|N (비밀번호 연속 실패 초과 시 Y) */
    @Column(name = "lock_yn", nullable = false, length = 1)
    @Builder.Default
    private String lockYn = "N";

    /** 잠금 일시 (자동 해제 기준) */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /** 로그인 연속 실패 횟수 */
    @Column(name = "login_fail_count", nullable = false)
    @Builder.Default
    private int loginFailCount = 0;

    /** 2차 인증 Y|N */
    @Column(name = "mfa_enabled_yn", nullable = false, length = 1)
    @Builder.Default
    private String mfaEnabledYn = "N";

    /** PII 암호화 Y|N */
    @Column(name = "encrypt_yn", nullable = false, length = 1)
    @Builder.Default
    private String encryptYn = "N";

    /** 겸직여부 Y|N */
    @Column(name = "concurrent_yn", nullable = false, length = 1)
    @Builder.Default
    private String concurrentYn = "N";

    /** 계정 사용 시작일 */
    @Column(name = "valid_start_date")
    private LocalDate validStartDate;

    /** 계정 사용 종료일 (NULL=무제한) */
    @Column(name = "valid_end_date")
    private LocalDate validEndDate;

    /** 생성일 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 생성자 */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /** 수정일 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 수정자 */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /** 삭제일 (소프트 삭제) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 삭제자 */
    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    /** 담당 업무 */
    @Column(name = "reserve_field_1") private String jobDuty;
    @Column(name = "reserve_field_2") private String reserveField2;
    @Column(name = "reserve_field_3") private String reserveField3;
    @Column(name = "reserve_field_4") private String reserveField4;
    @Column(name = "reserve_field_5") private String reserveField5;

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
