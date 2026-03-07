package com.idstory.policy.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.policy.entity.SystemPolicy;
import com.idstory.policy.entity.SystemPolicyHistory;
import com.idstory.policy.entity.SystemPolicyId;
import com.idstory.policy.repository.SystemPolicyHistoryRepository;
import com.idstory.policy.repository.SystemPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 통합 정책 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemPolicyService {

    private final SystemPolicyRepository policyRepository;
    private final SystemPolicyHistoryRepository historyRepository;

    // ── 조회 헬퍼 ──────────────────────────────────────────────

    /**
     * 정책 값을 문자열로 반환합니다. 없으면 defaultValue를 반환합니다.
     */
    @Transactional(readOnly = true)
    public String getString(String group, String key, String defaultValue) {
        return policyRepository.findById(new SystemPolicyId(group, key))
                .map(SystemPolicy::getPolicyValue)
                .orElse(defaultValue);
    }

    /**
     * 정책 값을 정수로 반환합니다. 없거나 파싱 실패 시 defaultValue를 반환합니다.
     */
    @Transactional(readOnly = true)
    public int getInt(String group, String key, int defaultValue) {
        String val = getString(group, key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 정책 값을 boolean으로 반환합니다. 없으면 defaultValue를 반환합니다.
     */
    @Transactional(readOnly = true)
    public boolean getBoolean(String group, String key, boolean defaultValue) {
        String val = getString(group, key, null);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val.trim());
    }

    // ── 그룹 조회 ───────────────────────────────────────────────

    /**
     * 그룹별 전체 정책 목록을 Map(key → value)으로 반환합니다.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getPolicyMap(String group) {
        return policyRepository.findByPolicyGroupOrderByPolicyKey(group).stream()
                .collect(Collectors.toMap(SystemPolicy::getPolicyKey, SystemPolicy::getPolicyValue));
    }

    /**
     * 그룹별 전체 정책 엔티티 목록을 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<SystemPolicy> getPolicies(String group) {
        return policyRepository.findByPolicyGroupOrderByPolicyKey(group);
    }

    // ── 저장 ────────────────────────────────────────────────────

    /**
     * 그룹 전체 정책을 Map으로 저장하고 이력을 기록합니다.
     *
     * @param group   정책 그룹
     * @param updates key → newValue 맵
     * @param changedBy 변경자
     */
    @Transactional
    public void saveAll(String group, Map<String, String> updates, String changedBy) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String newVal = entry.getValue() == null ? "" : entry.getValue().trim();

            Optional<SystemPolicy> existing = policyRepository.findById(new SystemPolicyId(group, key));
            String oldVal = existing.map(SystemPolicy::getPolicyValue).orElse(null);

            if (existing.isPresent()) {
                SystemPolicy policy = existing.get();
                if (!newVal.equals(policy.getPolicyValue())) {
                    policy.setPolicyValue(newVal);
                    policy.setUpdatedBy(changedBy);
                    policyRepository.save(policy);
                    recordHistory(group, key, oldVal, newVal, changedBy);
                }
            } else {
                SystemPolicy policy = SystemPolicy.builder()
                        .policyGroup(group)
                        .policyKey(key)
                        .policyValue(newVal)
                        .updatedBy(changedBy)
                        .build();
                policyRepository.save(policy);
                recordHistory(group, key, null, newVal, changedBy);
            }
        }
        log.info("[SystemPolicyService] 정책 저장 완료 - group: {}, 항목 수: {}, 변경자: {}",
                group, updates.size(), changedBy);
    }

    // ── 이력 ────────────────────────────────────────────────────

    /**
     * 최근 정책 변경 이력 조회 (최대 100건).
     */
    @Transactional(readOnly = true)
    public List<SystemPolicyHistory> getRecentHistory(int limit) {
        return historyRepository.findAllByOrderByChangedAtDesc(PageRequest.of(0, limit));
    }

    private void recordHistory(String group, String key, String oldVal, String newVal, String changedBy) {
        SystemPolicyHistory hist = SystemPolicyHistory.builder()
                .histOid(OidGenerator.generate())
                .policyGroup(group)
                .policyKey(key)
                .oldValue(oldVal)
                .newValue(newVal)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .build();
        historyRepository.save(hist);
    }
}
