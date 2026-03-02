package com.idstory.history.repository;

import com.idstory.history.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 로그인 이력 JPA 리포지토리
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, String> {

    /**
     * 다중 조건 필터 + 페이징 조회
     *
     * @param userId     아이디 키워드 (null=전체)
     * @param actionType 이벤트 유형 (null=전체)
     * @param dateFrom   조회 시작 일시 (null=전체)
     * @param dateTo     조회 종료 일시 (null=전체)
     */
    @Query(value = """
            SELECT h FROM LoginHistory h
            WHERE (:userId     IS NULL OR h.userId     LIKE %:userId%)
              AND (:actionType IS NULL OR h.actionType  = :actionType)
              AND (:dateFrom   IS NULL OR h.performedAt >= :dateFrom)
              AND (:dateTo     IS NULL OR h.performedAt <= :dateTo)
            ORDER BY h.performedAt DESC
            """,
           countQuery = """
            SELECT COUNT(h) FROM LoginHistory h
            WHERE (:userId     IS NULL OR h.userId     LIKE %:userId%)
              AND (:actionType IS NULL OR h.actionType  = :actionType)
              AND (:dateFrom   IS NULL OR h.performedAt >= :dateFrom)
              AND (:dateTo     IS NULL OR h.performedAt <= :dateTo)
            """)
    Page<LoginHistory> findByFilter(
            @Param("userId")     String userId,
            @Param("actionType") String actionType,
            @Param("dateFrom")   LocalDateTime dateFrom,
            @Param("dateTo")     LocalDateTime dateTo,
            Pageable pageable);
}
