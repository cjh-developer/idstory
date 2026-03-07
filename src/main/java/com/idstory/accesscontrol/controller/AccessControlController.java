package com.idstory.accesscontrol.controller;

import com.idstory.accesscontrol.entity.AccessControlRule;
import com.idstory.accesscontrol.service.AccessControlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IP/MAC 접근 제어 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/auth/access-control")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccessControlController {

    private final AccessControlService accessControlService;

    // ── 페이지 조회 ──────────────────────────────────────────────

    @GetMapping
    public String view(HttpServletRequest request, Model model) {
        model.addAttribute("ipEnabled",   accessControlService.isIpControlEnabled());
        model.addAttribute("macEnabled",  accessControlService.isMacControlEnabled());
        model.addAttribute("ipRules",     accessControlService.findAllByType("IP"));
        model.addAttribute("macRules",    accessControlService.findAllByType("MAC"));
        model.addAttribute("clientIp",    getClientIp(request));
        model.addAttribute("clientMac",   request.getHeader("X-Device-MAC"));
        model.addAttribute("historyList",
                accessControlService.findBlockHistory(PageRequest.of(0, 50)).getContent());
        return "main/auth/access-control";
    }

    // ── IP/MAC 활성화 토글 ────────────────────────────────────────

    @PostMapping("/ip/toggle")
    public String toggleIp(@RequestParam boolean enabled,
                           @AuthenticationPrincipal UserDetails principal,
                           RedirectAttributes ra) {
        accessControlService.setIpControlEnabled(enabled, username(principal));
        ra.addFlashAttribute("successMsg",
                "IP 접근 제어가 " + (enabled ? "활성화" : "비활성화") + "되었습니다.");
        return "redirect:/auth/access-control";
    }

    @PostMapping("/mac/toggle")
    public String toggleMac(@RequestParam boolean enabled,
                            @AuthenticationPrincipal UserDetails principal,
                            RedirectAttributes ra) {
        accessControlService.setMacControlEnabled(enabled, username(principal));
        ra.addFlashAttribute("successMsg",
                "MAC 접근 제어가 " + (enabled ? "활성화" : "비활성화") + "되었습니다.");
        return "redirect:/auth/access-control";
    }

    // ── 규칙 등록 (AJAX JSON) ────────────────────────────────────

    @PostMapping("/rules/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addRule(
            @RequestParam String controlType,
            @RequestParam(required = false) String ipVersion,
            @RequestParam String ruleValue,
            @RequestParam(required = false, defaultValue = "") String description,
            @AuthenticationPrincipal UserDetails principal) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            AccessControlRule rule = accessControlService.addRule(
                    controlType, ipVersion, ruleValue, description, username(principal));
            result.put("success", true);
            result.put("ruleOid",     rule.getRuleOid());
            result.put("ruleValue",   rule.getRuleValue());
            result.put("ipVersion",   rule.getIpVersion());
            result.put("description", rule.getDescription());
            result.put("useYn",       rule.getUseYn());
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── 규칙 개별 토글 (AJAX JSON) ───────────────────────────────

    @PostMapping("/rules/{ruleOid}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleRule(
            @PathVariable String ruleOid,
            @AuthenticationPrincipal UserDetails principal) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            accessControlService.toggleRule(ruleOid, username(principal));
            result.put("success", true);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── 규칙 삭제 (AJAX JSON) ────────────────────────────────────

    @PostMapping("/rules/{ruleOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable String ruleOid) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            accessControlService.deleteRule(ruleOid);
            result.put("success", true);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── 차단 이력 조회 (AJAX JSON) ───────────────────────────────

    @GetMapping("/api/history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getHistory() {
        List<Map<String, Object>> list = accessControlService
                .findBlockHistory(PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(h -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("controlType", h.getControlType());
                    m.put("requestVal",  h.getRequestVal());
                    m.put("requestUri",  h.getRequestUri());
                    m.put("blockedAt",   h.getBlockedAt() != null
                            ? h.getBlockedAt().toString().replace("T", " ").substring(0, 16) : "");
                    return m;
                }).toList();
        return ResponseEntity.ok(list);
    }

    // ── 유틸 ────────────────────────────────────────────────────

    private String username(UserDetails principal) {
        return principal != null ? principal.getUsername() : "SYSTEM";
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
