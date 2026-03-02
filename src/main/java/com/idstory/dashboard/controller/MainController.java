package com.idstory.dashboard.controller;

import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 로그인 후 메인 페이지 컨트롤러
 * - 모든 경로는 Spring Security 인증 필요 (SecurityConfig 설정)
 */
@Slf4j
@Controller
@RequestMapping("/main")
@RequiredArgsConstructor
public class MainController {

    private final SysUserRepository sysUserRepository;

    /** 대시보드 - templates/main/dashboard.html */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        log.info("[MainController] 대시보드 접근 - username: {}",
                authentication != null ? authentication.getName() : "unknown");

        model.addAttribute("statTotal",    sysUserRepository.countByDeletedAtIsNull());
        model.addAttribute("statActive",   sysUserRepository.countByDeletedAtIsNullAndUseYn("Y"));
        model.addAttribute("statAdmin",    sysUserRepository.countByDeletedAtIsNullAndRole("ADMIN"));
        model.addAttribute("statInactive", sysUserRepository.countByDeletedAtIsNullAndUseYn("N"));

        return "main/dashboard";
    }

    /** 홈 - templates/main/home.html */
    @GetMapping("/home")
    public String home(Authentication authentication) {
        log.info("[MainController] 홈 접근 - username: {}",
                authentication != null ? authentication.getName() : "unknown");
        return "main/home";
    }
}
