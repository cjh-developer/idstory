package com.idstory.history.repository;

import com.idstory.history.entity.UserAccountHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 사용자 계정 이력 JPA 리포지토리
 */
@Repository
public interface UserAccountHistoryRepository extends JpaRepository<UserAccountHistory, String> {

    /**
     * 필터 조건으로 이력 목록 페이징 조회
     */
    @Query("""
            SELECT h FROM UserAccountHistory h
            WHERE (:actionType IS NULL OR h.actionType = :actionType)
              AND (:username IS NULL OR h.targetUsername LIKE %:username%)
              AND (:dateFrom IS NULL OR h.performedAt >= :dateFrom)
              AND (:dateTo IS NULL OR h.performedAt <= :dateTo)
            ORDER BY h.performedAt DESC
            """)
    Page<UserAccountHistory> findByFilter(
            @Param("actionType") String actionType,
            @Param("username") String username,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
}
