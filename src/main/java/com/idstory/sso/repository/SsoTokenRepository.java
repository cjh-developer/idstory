package com.idstory.sso.repository;

import com.idstory.sso.entity.SsoToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SsoTokenRepository extends JpaRepository<SsoToken, String> {

    Optional<SsoToken> findByJti(String jti);

    @Query("SELECT t FROM SsoToken t WHERE t.clientOid = :clientOid AND t.userOid = :userOid AND t.revokedAt IS NULL AND t.expiresAt > :now")
    List<SsoToken> findActiveByClientOidAndUserOid(
            @Param("clientOid") String clientOid,
            @Param("userOid")   String userOid,
            @Param("now")       LocalDateTime now);

    @Query(value = """
        SELECT t FROM SsoToken t
        WHERE (:clientId   IS NULL OR t.clientId   = :clientId)
          AND (:userId     IS NULL OR t.userId     LIKE %:userId%)
          AND (:tokenType  IS NULL OR t.tokenType  = :tokenType)
          AND (:dateFrom   IS NULL OR t.issuedAt   >= :dateFrom)
          AND (:dateTo     IS NULL OR t.issuedAt   <= :dateTo)
        ORDER BY t.issuedAt DESC
        """,
        countQuery = """
        SELECT COUNT(t) FROM SsoToken t
        WHERE (:clientId   IS NULL OR t.clientId   = :clientId)
          AND (:userId     IS NULL OR t.userId     LIKE %:userId%)
          AND (:tokenType  IS NULL OR t.tokenType  = :tokenType)
          AND (:dateFrom   IS NULL OR t.issuedAt   >= :dateFrom)
          AND (:dateTo     IS NULL OR t.issuedAt   <= :dateTo)
        """)
    Page<SsoToken> findByFilter(
            @Param("clientId")  String clientId,
            @Param("userId")    String userId,
            @Param("tokenType") String tokenType,
            @Param("dateFrom")  LocalDateTime dateFrom,
            @Param("dateTo")    LocalDateTime dateTo,
            Pageable pageable);
}
