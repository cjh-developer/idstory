package com.idstory.login.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 로그인 컨트롤러
 * - GET /login → templates/index.html  (로그인 페이지)
 * - GET /      → redirect:/main/dashboard (로그인 성공 후 대시보드)
 */
@Slf4j
@Controller
public class LoginController {

    /**
     * 로그인 페이지 (templates/index.html)
     * - ?error=true  : 로그인 실패
     * - ?logout=true : 로그아웃 완료
     * - successKey   : Flash attribute (비밀번호 변경 성공 등)
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if ("true".equals(error)) {
            log.debug("[LoginController] 로그인 실패 후 로그인 페이지 표시");
        } else if ("true".equals(logout)) {
            log.debug("[LoginController] 로그아웃 완료 후 로그인 페이지 표시");
        } else {
            log.debug("[LoginController] 로그인 페이지 표시");
        }
        return "index";
    }

    /**
     * 루트 접근 시 대시보드로 리다이렉트
     * (Spring Security 로그인 성공 후 defaultSuccessUrl("/") 대응)
     */
    @GetMapping("/")
    public String root() {
        log.debug("[LoginController] 루트(/) 접근 → 대시보드 리다이렉트");
        return "redirect:/main/dashboard";
    }
}
