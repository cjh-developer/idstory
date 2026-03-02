package com.idstory.policy.repository;

import com.idstory.policy.entity.PasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 비밀번호 정책 JPA 리포지토리
 */
@Repository
public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, String> {
}
