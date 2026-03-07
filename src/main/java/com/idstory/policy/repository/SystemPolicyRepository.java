package com.idstory.policy.repository;

import com.idstory.policy.entity.SystemPolicy;
import com.idstory.policy.entity.SystemPolicyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 통합 정책 JPA 리포지토리
 */
@Repository
public interface SystemPolicyRepository extends JpaRepository<SystemPolicy, SystemPolicyId> {

    /** 그룹별 전체 정책 목록 */
    List<SystemPolicy> findByPolicyGroupOrderByPolicyKey(String policyGroup);
}
