package com.idstory.common.security;

import com.idstory.history.service.LoginHistoryService;
import com.idstory.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Spring Security 인증 이벤트 리스너
 *
 * <ul>
 *   <li>로그인 성공 → 실패 횟수 초기화</li>
 *   <li>비밀번호 불일치 실패 → 실패 횟수 증가, 정책 초과 시 계정 자동 잠금</li>
 *   <li>모든 실패 → WARN 로그</li>
 * </ul>
 *
 * <p>로그아웃 로그는 SecurityConfig 의 LogoutSuccessHandler에서 처리합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventListener {

    private final SysUserService sysUserService;
    private final LoginHistoryService loginHistoryService;

    /**
     * 로그인 성공 — 연속 실패 횟수 초기화
     */
    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String ip = extractIp(auth);

        log.info("[AUTH] 로그인 성공 - userId: {}, 권한: {}, IP: {}",
                auth.getName(), auth.getAuthorities(), ip);

        sysUserService.handleLoginSuccess(auth.getName());
        loginHistoryService.log(auth.getName(), "LOGIN_SUCCESS", null, ip);
    }

    /**
     * 로그인 실패 — 전체 로그 기록 + 비밀번호 불일치 시 실패 횟수 처리
     *
     * <p>실패 사유 유형:</p>
     * <ul>
     *   <li>BadCredentialsException    — 비밀번호 불일치 (횟수 카운트)</li>
     *   <li>DisabledException         — use_yn=N (횟수 카운트 안 함)</li>
     *   <li>LockedException           — lock_yn=Y (횟수 카운트 안 함)</li>
     *   <li>UsernameNotFoundException — 존재하지 않는 계정 (횟수 카운트 안 함)</li>
     * </ul>
     */
    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String userId = (principal instanceof String s) ? s : principal.toString();
        String ip     = extractIp(event.getAuthentication());
        String reason = event.getException().getClass().getSimpleName();

        log.warn("[AUTH] 로그인 실패 - userId: {}, 사유: {}, IP: {}",
                userId, reason, ip);

        loginHistoryService.log(userId, "LOGIN_FAIL", reason, ip);

        // 비밀번호 불일치 시에만 실패 횟수 처리
        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            sysUserService.handleLoginFailure(userId, ip);
        }
    }

    /**
     * Authentication 객체에서 원격 IP 주소를 추출합니다.
     */
    private String extractIp(Authentication auth) {
        if (auth != null && auth.getDetails() instanceof WebAuthenticationDetails details) {
            return details.getRemoteAddress();
        }
        return "unknown";
    }
}
