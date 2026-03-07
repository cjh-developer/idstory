package com.idstory.policy.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 비밀번호 정책 컨트롤러 (레거시 URL → 새 정책 관리 페이지로 리다이렉트)
 */
@Controller
@RequestMapping("/policy")
@PreAuthorize("hasRole('ADMIN')")
public class PasswordPolicyController {

    @GetMapping("/password")
    public String redirectToManage() {
        return "redirect:/policy/manage/password";
    }
}
