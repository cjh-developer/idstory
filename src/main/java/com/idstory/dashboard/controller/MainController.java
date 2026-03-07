package com.idstory.dashboard.controller;

import com.idstory.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 대시보드 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/main")
@RequiredArgsConstructor
public class MainController {

    private final DashboardService dashboardService;

    // ── 페이지 ───────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        log.info("[MainController] 대시보드 접근 - {}", authentication != null ? authentication.getName() : "unknown");
        // 최초 렌더링용 기본 통계 (캐시 없이 서버사이드 바인딩)
        model.addAllAttributes(dashboardService.getStatCounts());
        return "main/dashboard";
    }

    @GetMapping("/home")
    public String home() {
        return "main/home";
    }

    // ── API ──────────────────────────────────────────────────────────

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Object> apiStats() {
        return ResponseEntity.ok(dashboardService.getStatCounts());
    }

    @GetMapping("/api/monthly-trend")
    @ResponseBody
    public ResponseEntity<Object> apiMonthlyTrend() {
        return ResponseEntity.ok(dashboardService.getMonthlyTrend());
    }

    @GetMapping("/api/client-users")
    @ResponseBody
    public ResponseEntity<Object> apiClientUsers() {
        return ResponseEntity.ok(dashboardService.getClientUserCounts());
    }

    @GetMapping("/api/recent-logins")
    @ResponseBody
    public ResponseEntity<Object> apiRecentLogins() {
        return ResponseEntity.ok(dashboardService.getRecentLogins());
    }

    @GetMapping("/api/account-events")
    @ResponseBody
    public ResponseEntity<Object> apiAccountEvents() {
        return ResponseEntity.ok(dashboardService.getAccountEvents());
    }

    @GetMapping("/api/security-alerts")
    @ResponseBody
    public ResponseEntity<Object> apiSecurityAlerts() {
        return ResponseEntity.ok(dashboardService.getSecurityAlerts());
    }

    @GetMapping("/api/admins")
    @ResponseBody
    public ResponseEntity<Object> apiAdmins() {
        return ResponseEntity.ok(dashboardService.getAdminList());
    }
}
