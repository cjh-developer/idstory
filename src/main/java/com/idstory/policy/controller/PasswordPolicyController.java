package com.idstory.policy.controller;

import com.idstory.policy.service.PasswordPolicyService;
import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 비밀번호 정책 관리 컨트롤러
 */
@Controller
@RequestMapping("/policy")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PasswordPolicyController {

    private final PasswordPolicyService passwordPolicyService;
    private final SysUserRepository sysUserRepository;

    /**
     * 비밀번호 정책 조회
     * GET /policy/password
     */
    @GetMapping("/password")
    public String view(Model model) {
        model.addAttribute("maxLoginFailCount", passwordPolicyService.getMaxLoginFailCount());
        model.addAttribute("lockedUserCount",
                sysUserRepository.countByLockYn("Y"));
        return "main/policy/password";
    }

    /**
     * 비밀번호 정책 저장
     * POST /policy/password
     */
    @PostMapping("/password")
    public String save(
            @RequestParam int maxLoginFailCount,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        try {
            String updatedBy = (principal != null) ? principal.getUsername() : "SYSTEM";
            passwordPolicyService.updateMaxLoginFailCount(maxLoginFailCount, updatedBy);
            redirectAttributes.addFlashAttribute("successMsg",
                    "비밀번호 정책이 저장되었습니다. (최대 실패 횟수: " + maxLoginFailCount + "회)");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/policy/password";
    }
}
