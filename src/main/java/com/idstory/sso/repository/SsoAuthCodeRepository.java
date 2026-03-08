package com.idstory.sso.repository;

import com.idstory.sso.entity.SsoAuthCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SsoAuthCodeRepository extends JpaRepository<SsoAuthCode, String> {

    Optional<SsoAuthCode> findByAuthCode(String authCode);

    @Query(value = """
        SELECT a FROM SsoAuthCode a
        WHERE (:clientId IS NULL OR a.clientId = :clientId)
          AND (:userId   IS NULL OR a.userId   LIKE %:userId%)
          AND (:status   IS NULL OR a.status   = :status)
          AND (:dateFrom IS NULL OR a.issuedAt >= :dateFrom)
          AND (:dateTo   IS NULL OR a.issuedAt <= :dateTo)
        ORDER BY a.issuedAt DESC
        """,
        countQuery = """
        SELECT COUNT(a) FROM SsoAuthCode a
        WHERE (:clientId IS NULL OR a.clientId = :clientId)
          AND (:userId   IS NULL OR a.userId   LIKE %:userId%)
          AND (:status   IS NULL OR a.status   = :status)
          AND (:dateFrom IS NULL OR a.issuedAt >= :dateFrom)
          AND (:dateTo   IS NULL OR a.issuedAt <= :dateTo)
        """)
    Page<SsoAuthCode> findByFilter(
            @Param("clientId") String clientId,
            @Param("userId")   String userId,
            @Param("status")   String status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo")   LocalDateTime dateTo,
            Pageable pageable);

    /** 만료된 ISSUED 코드를 EXPIRED로 일괄 처리 (배치용) */
    @Modifying
    @Query("UPDATE SsoAuthCode a SET a.status = 'EXPIRED' WHERE a.status = 'ISSUED' AND a.expiresAt < :now")
    int expireCodes(@Param("now") LocalDateTime now);
}
