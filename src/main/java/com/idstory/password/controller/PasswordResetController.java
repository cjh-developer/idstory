package com.idstory.password.controller;

import com.idstory.password.entity.PasswordResetToken;
import com.idstory.password.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 비밀번호 초기화 컨트롤러
 *
 * <p>URL 구조</p>
 * <ul>
 *   <li>GET  /password-reset              → 초기화 요청 폼 (아이디 + 이메일 입력)</li>
 *   <li>POST /password-reset              → 토큰 생성 + 초기화 링크 표시</li>
 *   <li>GET  /password-reset/confirm      → 새 비밀번호 입력 폼 (token 파라미터 필요)</li>
 *   <li>POST /password-reset/confirm      → 비밀번호 변경 처리</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/password-reset")
public class PasswordResetController {

    private final PasswordResetService resetService;

    public PasswordResetController(PasswordResetService resetService) {
        this.resetService = resetService;
    }

    // ── 1. 초기화 요청 폼 ────────────────────────────────────────────────────────

    @GetMapping
    public String showRequestForm(HttpServletRequest request) {
        log.debug("[PasswordResetController] 비밀번호 초기화 요청 폼 표시 - IP: {}",
                request.getRemoteAddr());
        return "login/password-reset-request";
    }

    // ── 2. 토큰 생성 처리 ────────────────────────────────────────────────────────

    @PostMapping
    public String processRequest(
            @RequestParam String username,
            @RequestParam String email,
            HttpServletRequest request,
            Model model) {

        String ip = request.getRemoteAddr();
        log.info("[PasswordResetController] 비밀번호 초기화 요청 - username: {}, IP: {}",
                username.trim(), ip);

        try {
            String token = resetService.createResetToken(username.trim(), email.trim());

            // 현재 서버 주소로 초기화 URL 생성
            String baseUrl = request.getScheme() + "://"
                    + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath();
            String resetUrl = baseUrl + "/password-reset/confirm?token=" + token;

            model.addAttribute("resetUrl", resetUrl);
            model.addAttribute("username", username);

            log.info("[PasswordResetController] 초기화 링크 생성 완료 - username: {}, IP: {}",
                    username.trim(), ip);
            return "login/password-reset-sent";

        } catch (IllegalArgumentException e) {
            log.warn("[PasswordResetController] 초기화 요청 실패 - username: {}, 사유: {}, IP: {}",
                    username.trim(), e.getMessage(), ip);
            model.addAttribute("errorKey", e.getMessage());
            return "login/password-reset-request";
        }
    }

    // ── 3. 새 비밀번호 입력 폼 ───────────────────────────────────────────────────

    @GetMapping("/confirm")
    public String showConfirmForm(@RequestParam String token,
                                  HttpServletRequest request,
                                  Model model) {
        String ip = request.getRemoteAddr();
        log.info("[PasswordResetController] 비밀번호 변경 폼 접근 - IP: {}", ip);

        try {
            PasswordResetToken resetToken = resetService.validateToken(token);
            model.addAttribute("token", token);
            model.addAttribute("username", resetToken.getUsername());
            log.debug("[PasswordResetController] 토큰 유효 - username: {}", resetToken.getUsername());
            return "login/password-reset-form";

        } catch (IllegalArgumentException e) {
            log.warn("[PasswordResetController] 토큰 검증 실패 - 사유: {}, IP: {}",
                    e.getMessage(), ip);
            model.addAttribute("errorKey", e.getMessage());
            return "login/password-reset-form";
        }
    }

    // ── 4. 비밀번호 변경 처리 ────────────────────────────────────────────────────

    @PostMapping("/confirm")
    public String processConfirm(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {

        String ip = request.getRemoteAddr();

        // 비밀번호 확인 검증
        if (!newPassword.equals(confirmPassword)) {
            log.warn("[PasswordResetController] 비밀번호 불일치 - IP: {}", ip);
            model.addAttribute("token", token);
            model.addAttribute("errorKey", "reset.error.mismatch");
            return "login/password-reset-form";
        }

        // 비밀번호 최소 길이 검증
        if (newPassword.length() < 4) {
            log.warn("[PasswordResetController] 비밀번호 길이 부족 - IP: {}", ip);
            model.addAttribute("token", token);
            model.addAttribute("errorKey", "reset.error.tooShort");
            return "login/password-reset-form";
        }

        try {
            resetService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("successKey", "reset.success");

            // 로그인 상태이면 대시보드로, 비로그인이면 로그인 페이지로
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)) {
                log.info("[PasswordResetController] 비밀번호 변경 완료 (로그인 중) - username: {}, IP: {}",
                        auth.getName(), ip);
                return "redirect:/main/dashboard";
            }
            log.info("[PasswordResetController] 비밀번호 변경 완료 (비로그인) - IP: {}", ip);
            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            log.warn("[PasswordResetController] 비밀번호 변경 실패 - 사유: {}, IP: {}",
                    e.getMessage(), ip);
            model.addAttribute("token", token);
            model.addAttribute("errorKey", e.getMessage());
            return "login/password-reset-form";
        }
    }
}
