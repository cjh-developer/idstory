package com.idstory.policy.scheduler;

import com.idstory.history.service.UserAccountHistoryService;
import com.idstory.policy.service.SystemPolicyService;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 계정 정책 스케줄러
 * - 잠금 자동 해제 (ACCOUNT_POLICY.ACCT_LOCK_AUTO_RELEASE, ADMIN_POLICY.ADMIN_LOCK_AUTO_RELEASE_MINS)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountPolicyScheduler {

    private final SystemPolicyService systemPolicyService;
    private final SysUserRepository sysUserRepository;
    private final UserAccountHistoryService historyService;

    /**
     * 1분마다 잠금 계정 자동 해제를 실행합니다.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void autoUnlockAccounts() {
        int releaseMins = systemPolicyService.getInt("ACCOUNT_POLICY", "ACCT_LOCK_AUTO_RELEASE", 0);
        int adminRelease = systemPolicyService.getInt("ADMIN_POLICY", "ADMIN_LOCK_AUTO_RELEASE_MINS", 0);

        if (releaseMins > 0) {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(releaseMins);
            List<SysUser> candidates = sysUserRepository.findLockedBefore(threshold);
            candidates.stream()
                    .filter(u -> !"ADMIN".equals(u.getRole()))
                    .forEach(u -> unlock(u, "일반 사용자 잠금 자동 해제 (정책: " + releaseMins + "분)"));
        }

        if (adminRelease > 0) {
            LocalDateTime adminThreshold = LocalDateTime.now().minusMinutes(adminRelease);
            List<SysUser> adminCandidates = sysUserRepository.findLockedBefore(adminThreshold);
            adminCandidates.stream()
                    .filter(u -> "ADMIN".equals(u.getRole()))
                    .forEach(u -> unlock(u, "관리자 계정 잠금 자동 해제 (정책: " + adminRelease + "분)"));
        }
    }

    private void unlock(SysUser user, String detail) {
        user.setLockYn("N");
        user.setLockedAt(null);
        user.setLoginFailCount(0);
        sysUserRepository.save(user);

        historyService.log(user.getOid(), user.getUserId(), "UNLOCK", detail, "SYSTEM", null);
        log.info("[AccountPolicyScheduler] 계정 자동 해제 - userId: {}", user.getUserId());
    }
}
