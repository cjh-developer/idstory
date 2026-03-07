package com.idstory.policy.service;

import com.idstory.policy.entity.PasswordPolicy;
import com.idstory.policy.repository.PasswordPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 비밀번호 정책 서비스 (하위호환 유지 — 내부적으로 SystemPolicyService 위임)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    /** 로그인 최대 실패 횟수 정책 키 */
    public static final String KEY_MAX_LOGIN_FAIL = "MAX_LOGIN_FAIL_COUNT";

    /** 로그인 최대 실패 횟수 기본값 */
    private static final int DEFAULT_MAX_LOGIN_FAIL = 5;

    private final PasswordPolicyRepository repository;
    private final SystemPolicyService systemPolicyService;

    /**
     * 로그인 최대 실패 허용 횟수를 반환합니다.
     * ids_iam_policy의 PASSWORD_POLICY.MAX_LOGIN_FAIL_COUNT를 우선 사용합니다.
     */
    @Transactional(readOnly = true)
    public int getMaxLoginFailCount() {
        return systemPolicyService.getInt("PASSWORD_POLICY", KEY_MAX_LOGIN_FAIL, DEFAULT_MAX_LOGIN_FAIL);
    }

    /**
     * 전체 정책 목록을 반환합니다 (ids_iam_pwd_policy 기반, 레거시).
     */
    @Transactional(readOnly = true)
    public List<PasswordPolicy> findAll() {
        return repository.findAll();
    }

    /**
     * 로그인 최대 실패 횟수 정책을 저장합니다.
     * ids_iam_policy와 ids_iam_pwd_policy 양쪽에 저장합니다.
     *
     * @param count      새 허용 횟수 (1 이상)
     * @param updatedBy  수정자 userId
     */
    @Transactional
    public void updateMaxLoginFailCount(int count, String updatedBy) {
        if (count < 1) {
            throw new IllegalArgumentException("최대 실패 횟수는 1 이상이어야 합니다.");
        }
        // ids_iam_policy에 저장 (우선)
        systemPolicyService.saveAll("PASSWORD_POLICY",
                Map.of(KEY_MAX_LOGIN_FAIL, String.valueOf(count)), updatedBy);

        // ids_iam_pwd_policy 레거시 테이블에도 동기화
        PasswordPolicy policy = repository.findById(KEY_MAX_LOGIN_FAIL)
                .orElseGet(() -> PasswordPolicy.builder()
                        .policyKey(KEY_MAX_LOGIN_FAIL)
                        .description("로그인 연속 실패 허용 횟수 (초과 시 계정 자동 잠금)")
                        .build());
        policy.setPolicyValue(String.valueOf(count));
        policy.setUpdatedAt(LocalDateTime.now());
        policy.setUpdatedBy(updatedBy);
        repository.save(policy);

        log.info("[PasswordPolicyService] {} 정책 변경 - 값: {}, 수정자: {}",
                KEY_MAX_LOGIN_FAIL, count, updatedBy);
    }
}
