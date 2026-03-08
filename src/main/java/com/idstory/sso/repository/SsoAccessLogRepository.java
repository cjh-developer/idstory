package com.idstory.sso.repository;

import com.idstory.sso.entity.SsoAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SsoAccessLogRepository extends JpaRepository<SsoAccessLog, String> {

    @Query(value = """
        SELECT l FROM SsoAccessLog l
        WHERE (:clientId  IS NULL OR l.clientId  = :clientId)
          AND (:userId    IS NULL OR l.userId    LIKE %:userId%)
          AND (:action    IS NULL OR l.action    = :action)
          AND (:status    IS NULL OR l.status    = :status)
          AND (:ipAddress IS NULL OR l.ipAddress LIKE %:ipAddress%)
          AND (:dateFrom  IS NULL OR l.accessedAt >= :dateFrom)
          AND (:dateTo    IS NULL OR l.accessedAt <= :dateTo)
        ORDER BY l.accessedAt DESC
        """,
        countQuery = """
        SELECT COUNT(l) FROM SsoAccessLog l
        WHERE (:clientId  IS NULL OR l.clientId  = :clientId)
          AND (:userId    IS NULL OR l.userId    LIKE %:userId%)
          AND (:action    IS NULL OR l.action    = :action)
          AND (:status    IS NULL OR l.status    = :status)
          AND (:ipAddress IS NULL OR l.ipAddress LIKE %:ipAddress%)
          AND (:dateFrom  IS NULL OR l.accessedAt >= :dateFrom)
          AND (:dateTo    IS NULL OR l.accessedAt <= :dateTo)
        """)
    Page<SsoAccessLog> findByFilter(
            @Param("clientId")  String clientId,
            @Param("userId")    String userId,
            @Param("action")    String action,
            @Param("status")    String status,
            @Param("ipAddress") String ipAddress,
            @Param("dateFrom")  LocalDateTime dateFrom,
            @Param("dateTo")    LocalDateTime dateTo,
            Pageable pageable);
}
