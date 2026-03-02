package com.idstory.password.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 비밀번호 초기화 토큰 엔티티
 * 매핑 테이블: ids_iam_pwd_reset_token
 */
@Entity
@Table(name = "ids_iam_pwd_reset_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID 토큰 (URL에 포함) */
    @Column(unique = true, nullable = false, length = 36)
    private String token;

    /** 초기화 대상 사용자 아이디 */
    @Column(nullable = false, length = 50)
    private String username;

    /** 토큰 만료 일시 */
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    /** 사용 여부 (한 번 사용하면 재사용 불가) */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 토큰 만료 여부 확인 */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    /** 토큰 유효 여부 확인 (미사용 + 미만료) */
    public boolean isValid() {
        return !this.used && !isExpired();
    }
}
