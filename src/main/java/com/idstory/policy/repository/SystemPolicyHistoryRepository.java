package com.idstory.policy.repository;

import com.idstory.policy.entity.SystemPolicyHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 정책 변경 이력 JPA 리포지토리
 */
@Repository
public interface SystemPolicyHistoryRepository extends JpaRepository<SystemPolicyHistory, String> {

    /** 최근 이력 조회 (변경일시 내림차순) */
    List<SystemPolicyHistory> findAllByOrderByChangedAtDesc(Pageable pageable);
}
