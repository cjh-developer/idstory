package com.idstory.password.repository;

import com.idstory.password.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** 특정 사용자의 미사용 토큰 전부 삭제 (새 토큰 발급 전 정리) */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.username = :username AND t.used = false")
    void deleteUnusedByUsername(String username);

    /** 만료된 토큰 일괄 삭제 (배치/스케줄러에서 활용 가능) */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
