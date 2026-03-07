package com.idstory.accesscontrol.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 접근 제어 차단 페이지 컨트롤러 (인증 불필요, SecurityConfig에서 permitAll 처리)
 */
@Controller
@RequestMapping("/access-denied")
public class AccessDeniedPageController {

    @GetMapping
    public String accessDenied(
            @RequestParam(required = false, defaultValue = "IP") String type,
            @RequestParam(required = false, defaultValue = "") String val,
            Model model) {
        model.addAttribute("blockType", type);
        model.addAttribute("blockVal",  val);
        return "error/access-denied";
    }
}
