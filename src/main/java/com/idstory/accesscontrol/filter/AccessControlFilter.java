package com.idstory.accesscontrol.filter;

import com.idstory.accesscontrol.service.AccessControlService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * IP/MAC 접근 제어 필터
 *
 * <p>Spring Security 필터 체인에 등록되며, 요청마다 IP·MAC 허용 여부를 확인합니다.</p>
 * <p>우회 경로: /login, /access-denied, /css/**, /js/**, /images/**, /font/**,
 *              /auth/access-control/**, /password-reset/**</p>
 *
 * <p>MAC 주소는 {@code X-Device-MAC} 헤더에서 읽습니다.
 * 클라이언트(VPN·프록시·에이전트)가 해당 헤더를 주입해야 합니다.</p>
 */
@Slf4j
@Component
public class AccessControlFilter extends OncePerRequestFilter {

    /** MAC 주소 헤더 이름 */
    private static final String MAC_HEADER = "X-Device-MAC";

    private final AccessControlService accessControlService;

    public AccessControlFilter(@Lazy AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/font/")
                || path.startsWith("/favicon")
                || path.startsWith("/error")
                || path.equals("/login")
                || path.equals("/logout")
                || path.equals("/access-denied")
                || path.startsWith("/password-reset")
                || path.startsWith("/auth/access-control"); // 관리 페이지는 항상 허용
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        try {
            // ── IP 접근 제어 ─────────────────────────────────────
            if (accessControlService.isIpControlEnabled()) {
                String clientIp = getClientIp(request);
                if (!accessControlService.isIpAllowed(clientIp)) {
                    log.warn("[AccessControlFilter] IP 차단 - ip: {}, uri: {}",
                            clientIp, request.getRequestURI());
                    accessControlService.recordBlock("IP", clientIp, request.getRequestURI());
                    sendDenied(request, response, "IP", clientIp);
                    return;
                }
            }

            // ── MAC 접근 제어 ────────────────────────────────────
            if (accessControlService.isMacControlEnabled()) {
                String macHeader = request.getHeader(MAC_HEADER);
                if (macHeader == null || macHeader.isBlank()
                        || !accessControlService.isMacAllowed(macHeader)) {
                    String macVal = (macHeader != null) ? macHeader : "(헤더 없음)";
                    log.warn("[AccessControlFilter] MAC 차단 - mac: {}, uri: {}",
                            macVal, request.getRequestURI());
                    accessControlService.recordBlock("MAC", macVal, request.getRequestURI());
                    sendDenied(request, response, "MAC", macVal);
                    return;
                }
            }

        } catch (Exception e) {
            // DB 장애 등 예외 발생 시 fail-open: 접근 허용
            log.error("[AccessControlFilter] 접근 제어 처리 중 오류 (fail-open 허용): {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private void sendDenied(HttpServletRequest request,
                            HttpServletResponse response,
                            String type, String value) throws IOException {
        // AJAX 요청이면 JSON 403 반환, 그 외는 페이지 리다이렉트
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"ACCESS_DENIED\",\"message\":\"" + type + " 접근 제어에 의해 차단되었습니다.\"}");
        } else {
            response.sendRedirect(request.getContextPath() + "/access-denied?type=" + type
                    + "&val=" + java.net.URLEncoder.encode(value, "UTF-8"));
        }
    }

    /**
     * 실제 클라이언트 IP를 반환합니다.
     * X-Forwarded-For → X-Real-IP → RemoteAddr 순으로 확인합니다.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
