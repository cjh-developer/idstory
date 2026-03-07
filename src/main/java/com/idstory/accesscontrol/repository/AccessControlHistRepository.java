package com.idstory.accesscontrol.repository;

import com.idstory.accesscontrol.entity.AccessControlHist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 접근 제어 차단 이력 JPA 리포지토리
 */
@Repository
public interface AccessControlHistRepository extends JpaRepository<AccessControlHist, String> {

    /** 전체 차단 이력 (최신순) */
    Page<AccessControlHist> findAllByOrderByBlockedAtDesc(Pageable pageable);

    /** 유형별 차단 이력 (최신순) */
    Page<AccessControlHist> findByControlTypeOrderByBlockedAtDesc(String controlType, Pageable pageable);
}
