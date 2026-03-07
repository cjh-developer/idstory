package com.idstory.dashboard.service;

import com.idstory.admin.entity.SysAdmin;
import com.idstory.admin.repository.SysAdminRepository;
import com.idstory.client.repository.ClientRepository;
import com.idstory.history.entity.LoginHistory;
import com.idstory.history.entity.UserAccountHistory;
import com.idstory.history.repository.LoginHistoryRepository;
import com.idstory.history.repository.UserAccountHistoryRepository;
import com.idstory.policy.service.SystemPolicyService;
import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 대시보드 데이터 서비스
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SysUserRepository           sysUserRepository;
    private final LoginHistoryRepository      loginHistoryRepository;
    private final UserAccountHistoryRepository accountHistoryRepository;
    private final SysAdminRepository          sysAdminRepository;
    private final ClientRepository            clientRepository;
    private final SystemPolicyService         systemPolicyService;

    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter FMT_DATE     = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── 통계 카드 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getStatCounts() {
        long total      = sysUserRepository.countByDeletedAtIsNull();
        long active     = sysUserRepository.countByDeletedAtIsNullAndUseYn("Y");
        long inactive   = sysUserRepository.countByDeletedAtIsNullAndUseYn("N");
        long locked     = sysUserRepository.countByDeletedAtIsNullAndLockYn("Y");
        long mfaEnabled = sysUserRepository.countByDeletedAtIsNullAndMfaEnabledYn("Y");
        long adminCount = sysAdminRepository.count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total",       total);
        stats.put("active",      active);
        stats.put("inactive",    inactive);
        stats.put("locked",      locked);
        stats.put("mfaEnabled",  mfaEnabled);
        stats.put("mfaDisabled", total - mfaEnabled);
        stats.put("adminCount",  adminCount);
        return stats;
    }

    // ── 월별 사용자 등록 추이 ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlyTrend() {
        List<Object[]> rows = sysUserRepository.countMonthlyRegistrations();
        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", row[0]);
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());
    }

    // ── 시스템별 사용자 수 ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getClientUserCounts() {
        List<Object[]> rows = clientRepository.findClientUserCounts();
        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clientName", row[0]);
            m.put("userCount",  row[1]);
            return m;
        }).collect(Collectors.toList());
    }

    // ── 최근 로그인 이력 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentLogins() {
        return loginHistoryRepository.findTop20ByOrderByPerformedAtDesc().stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",      h.getUserId());
            m.put("actionType",  h.getActionType());
            m.put("ipAddress",   h.getIpAddress());
            m.put("failReason",  h.getFailReason());
            m.put("performedAt", h.getPerformedAt() != null ? h.getPerformedAt().format(FMT_DATETIME) : "");
            return m;
        }).collect(Collectors.toList());
    }

    // ── 계정 이벤트 로그 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAccountEvents() {
        return accountHistoryRepository.findTop20ByOrderByPerformedAtDesc().stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("targetUsername", h.getTargetUsername());
            m.put("actionType",     h.getActionType());
            m.put("performedBy",    h.getPerformedBy());
            m.put("actionDetail",   h.getActionDetail());
            m.put("performedAt",    h.getPerformedAt() != null ? h.getPerformedAt().format(FMT_DATETIME) : "");
            return m;
        }).collect(Collectors.toList());
    }

    // ── 보안 알림 ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getSecurityAlerts() {
        Map<String, Object> alerts = new LinkedHashMap<>();

        // 장기 미접속 (정책 기준일 이상 로그인 없는 활성 계정)
        int dormantDays = systemPolicyService.getInt("USER_POLICY", "USER_DORMANT_DAYS", 90);
        List<Object[]> dormantRows = sysUserRepository.findDormantUsers(dormantDays);
        List<Map<String, Object>> dormantList = dormantRows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",    row[1]);
            m.put("name",      row[2]);
            m.put("deptCode",  row[3]);
            m.put("lastLogin", row[4] != null ? row[4].toString().substring(0, 16) : "로그인 기록 없음");
            return m;
        }).collect(Collectors.toList());
        alerts.put("dormantUsers",  dormantList);
        alerts.put("dormantCount",  dormantList.size());

        // 24시간 내 로그인 실패 상위 사용자
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        long todayFailCount = loginHistoryRepository.countByActionTypeAndPerformedAtAfter("LOGIN_FAIL", since24h);
        List<Object[]> failRows = loginHistoryRepository.findTopFailUsersSince(since24h);
        List<Map<String, Object>> highFailList = failRows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",    row[0]);
            m.put("failCount", row[1]);
            return m;
        }).collect(Collectors.toList());
        alerts.put("todayFailCount", todayFailCount);
        alerts.put("highFailUsers",  highFailList);

        // 잠금 계정 수
        alerts.put("lockedCount", sysUserRepository.countByDeletedAtIsNullAndLockYn("Y"));

        // 최근 7일 관리자 권한 변경
        LocalDateTime since7d = LocalDateTime.now().minusDays(7);
        List<SysAdmin> recentAdmins = sysAdminRepository.findRecentGrants(since7d);
        List<Map<String, Object>> adminChanges = recentAdmins.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",    a.getUser().getUserId());
            m.put("name",      a.getUser().getName());
            m.put("grantedBy", a.getGrantedBy());
            m.put("grantedAt", a.getGrantedAt() != null ? a.getGrantedAt().format(FMT_DATETIME) : "");
            return m;
        }).collect(Collectors.toList());
        alerts.put("recentAdminGrants", adminChanges);

        return alerts;
    }

    // ── 관리자 목록 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdminList() {
        return sysAdminRepository.findByFilter(null, PageRequest.of(0, 50)).getContent().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",    a.getUser().getUserId());
            m.put("name",      a.getUser().getName());
            m.put("deptCode",  a.getUser().getDeptCode());
            m.put("grantedBy", a.getGrantedBy());
            m.put("grantedAt", a.getGrantedAt() != null ? a.getGrantedAt().format(FMT_DATE) : "");
            m.put("adminNote", a.getAdminNote());
            return m;
        }).collect(Collectors.toList());
    }
}
