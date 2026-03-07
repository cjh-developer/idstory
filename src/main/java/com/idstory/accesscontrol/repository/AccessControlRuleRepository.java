package com.idstory.accesscontrol.repository;

import com.idstory.accesscontrol.entity.AccessControlRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * IP/MAC 접근 제어 규칙 JPA 리포지토리
 */
@Repository
public interface AccessControlRuleRepository extends JpaRepository<AccessControlRule, String> {

    /** 유형별 전체 목록 (등록순) */
    List<AccessControlRule> findByControlTypeOrderByCreatedAtDesc(String controlType);

    /** 활성 규칙만 조회 */
    List<AccessControlRule> findByControlTypeAndUseYn(String controlType, String useYn);

    /** 중복 확인 */
    boolean existsByControlTypeAndRuleValue(String controlType, String ruleValue);
}
