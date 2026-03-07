package com.idstory.accesscontrol.service;

import com.idstory.accesscontrol.entity.AccessControlHist;
import com.idstory.accesscontrol.entity.AccessControlRule;
import com.idstory.accesscontrol.repository.AccessControlHistRepository;
import com.idstory.accesscontrol.repository.AccessControlRuleRepository;
import com.idstory.common.util.OidGenerator;
import com.idstory.policy.service.SystemPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * IP/MAC 접근 제어 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private static final String GROUP = "SYSTEM_POLICY";
    private static final String KEY_IP  = "IP_ACCESS_CONTROL_ENABLED";
    private static final String KEY_MAC = "MAC_ACCESS_CONTROL_ENABLED";

    private final AccessControlRuleRepository ruleRepository;
    private final AccessControlHistRepository histRepository;
    private final SystemPolicyService systemPolicyService;

    // ── 활성화 여부 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public boolean isIpControlEnabled() {
        return systemPolicyService.getBoolean(GROUP, KEY_IP, false);
    }

    @Transactional(readOnly = true)
    public boolean isMacControlEnabled() {
        return systemPolicyService.getBoolean(GROUP, KEY_MAC, false);
    }

    @Transactional
    public void setIpControlEnabled(boolean enabled, String changedBy) {
        systemPolicyService.saveAll(GROUP, Map.of(KEY_IP, String.valueOf(enabled)), changedBy);
        log.info("[AccessControlService] IP 접근 제어 {} - by: {}", enabled ? "활성화" : "비활성화", changedBy);
    }

    @Transactional
    public void setMacControlEnabled(boolean enabled, String changedBy) {
        systemPolicyService.saveAll(GROUP, Map.of(KEY_MAC, String.valueOf(enabled)), changedBy);
        log.info("[AccessControlService] MAC 접근 제어 {} - by: {}", enabled ? "활성화" : "비활성화", changedBy);
    }

    // ── 허용 여부 판단 (필터에서 호출) ──────────────────────────

    /**
     * 클라이언트 IP가 허용 목록에 포함되는지 확인합니다.
     * 활성 규칙 목록이 비어 있으면 모두 허용합니다 (lockout 방지).
     */
    @Transactional(readOnly = true)
    public boolean isIpAllowed(String clientIp) {
        List<AccessControlRule> rules = ruleRepository.findByControlTypeAndUseYn("IP", "Y");
        if (rules.isEmpty()) return true; // 목록 비어있으면 허용
        for (AccessControlRule rule : rules) {
            if (matchesIp(clientIp, rule.getRuleValue())) return true;
        }
        return false;
    }

    /**
     * MAC 주소가 허용 목록에 포함되는지 확인합니다.
     * 활성 규칙 목록이 비어 있으면 모두 허용합니다 (lockout 방지).
     */
    @Transactional(readOnly = true)
    public boolean isMacAllowed(String macAddress) {
        if (macAddress == null || macAddress.isBlank()) return false;
        List<AccessControlRule> rules = ruleRepository.findByControlTypeAndUseYn("MAC", "Y");
        if (rules.isEmpty()) return true; // 목록 비어있으면 허용
        String normalized = normalizeMac(macAddress);
        for (AccessControlRule rule : rules) {
            if (normalizeMac(rule.getRuleValue()).equals(normalized)) return true;
        }
        return false;
    }

    // ── 규칙 CRUD ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AccessControlRule> findAllByType(String controlType) {
        return ruleRepository.findByControlTypeOrderByCreatedAtDesc(controlType);
    }

    @Transactional(readOnly = true)
    public Page<AccessControlHist> findBlockHistory(Pageable pageable) {
        return histRepository.findAllByOrderByBlockedAtDesc(pageable);
    }

    @Transactional
    public AccessControlRule addRule(String controlType, String ipVersion,
                                     String ruleValue, String description,
                                     String createdBy) {
        String normalized = "MAC".equals(controlType)
                ? normalizeMac(ruleValue)
                : ruleValue.trim();

        if (ruleRepository.existsByControlTypeAndRuleValue(controlType, normalized)) {
            throw new IllegalArgumentException("이미 등록된 값입니다: " + normalized);
        }

        // IP 유효성 검사
        if ("IP".equals(controlType)) {
            validateIpOrCidr(normalized);
        } else {
            validateMac(normalized);
        }

        AccessControlRule rule = AccessControlRule.builder()
                .ruleOid(OidGenerator.generate())
                .controlType(controlType)
                .ruleValue(normalized)
                .ipVersion("IP".equals(controlType) ? ipVersion : null)
                .description(description)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        return ruleRepository.save(rule);
    }

    @Transactional
    public void toggleRule(String ruleOid, String updatedBy) {
        AccessControlRule rule = ruleRepository.findById(ruleOid)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다: " + ruleOid));
        rule.setUseYn("Y".equals(rule.getUseYn()) ? "N" : "Y");
        rule.setUpdatedBy(updatedBy);
        ruleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(String ruleOid) {
        if (!ruleRepository.existsById(ruleOid)) {
            throw new IllegalArgumentException("규칙을 찾을 수 없습니다: " + ruleOid);
        }
        ruleRepository.deleteById(ruleOid);
    }

    // ── 차단 이력 기록 ──────────────────────────────────────────

    @Transactional
    public void recordBlock(String controlType, String requestVal, String requestUri) {
        AccessControlHist hist = AccessControlHist.builder()
                .histOid(OidGenerator.generate())
                .controlType(controlType)
                .requestVal(requestVal)
                .requestUri(requestUri)
                .build();
        histRepository.save(hist);
    }

    // ── 내부 유틸리티 ───────────────────────────────────────────

    /**
     * IP 주소가 등록된 규칙(IP 또는 CIDR)과 일치하는지 확인합니다.
     */
    private boolean matchesIp(String clientIp, String ruleValue) {
        try {
            if (!ruleValue.contains("/")) {
                // 단순 IP 비교 (정규화 후 비교)
                return InetAddress.getByName(clientIp).equals(InetAddress.getByName(ruleValue));
            }
            // CIDR 매칭
            String[] parts = ruleValue.split("/", 2);
            String networkAddr = parts[0];
            int prefix = Integer.parseInt(parts[1].trim());

            InetAddress clientAddr = InetAddress.getByName(clientIp);
            InetAddress netAddr    = InetAddress.getByName(networkAddr);

            byte[] clientBytes  = clientAddr.getAddress();
            byte[] networkBytes = netAddr.getAddress();

            if (clientBytes.length != networkBytes.length) return false; // IPv4 vs IPv6 불일치

            int totalBits = clientBytes.length * 8;
            if (prefix < 0 || prefix > totalBits) return false;

            BigInteger clientInt  = new BigInteger(1, clientBytes);
            BigInteger networkInt = new BigInteger(1, networkBytes);
            BigInteger mask       = prefix == 0
                    ? BigInteger.ZERO
                    : BigInteger.ONE.shiftLeft(totalBits - prefix).subtract(BigInteger.ONE).not()
                                   .and(BigInteger.ONE.shiftLeft(totalBits).subtract(BigInteger.ONE));

            return clientInt.and(mask).equals(networkInt.and(mask));

        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("[AccessControlService] IP 매칭 오류 - client: {}, rule: {}", clientIp, ruleValue);
            return false;
        }
    }

    /**
     * MAC 주소를 대문자 콜론 형식(AA:BB:CC:DD:EE:FF)으로 정규화합니다.
     */
    public String normalizeMac(String mac) {
        if (mac == null) return "";
        String clean = mac.replaceAll("[:\\-\\s]", "").toUpperCase();
        if (clean.length() != 12) return mac.toUpperCase(); // 형식 불일치 시 원본 반환
        return String.join(":", clean.split("(?<=\\G.{2})"));
    }

    private void validateIpOrCidr(String value) {
        try {
            if (value.contains("/")) {
                String[] parts = value.split("/", 2);
                InetAddress.getByName(parts[0]);
                int prefix = Integer.parseInt(parts[1].trim());
                int maxPrefix = parts[0].contains(":") ? 128 : 32;
                if (prefix < 0 || prefix > maxPrefix) {
                    throw new IllegalArgumentException("CIDR 프리픽스 범위 오류: " + value);
                }
            } else {
                InetAddress.getByName(value);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("유효하지 않은 IP 주소입니다: " + value);
        }
    }

    private void validateMac(String mac) {
        if (!mac.matches("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")) {
            throw new IllegalArgumentException("유효하지 않은 MAC 주소 형식입니다 (AA:BB:CC:DD:EE:FF): " + mac);
        }
    }
}
