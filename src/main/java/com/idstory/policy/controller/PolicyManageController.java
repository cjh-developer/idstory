package com.idstory.policy.controller;

import com.idstory.policy.entity.SystemPolicy;
import com.idstory.policy.entity.SystemPolicyHistory;
import com.idstory.policy.service.SystemPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.util.MultiValueMap;

import java.util.*;

/**
 * 통합 정책 관리 컨트롤러
 * GET  /policy/manage/{tab}       — 탭 페이지 조회
 * POST /policy/manage/{tab}/save  — 탭 정책 저장
 */
@Controller
@RequestMapping("/policy/manage")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PolicyManageController {

    private static final List<String> VALID_TABS = List.of(
            "admin", "user", "password", "login", "account", "audit", "system", "history"
    );

    private static final Map<String, String> TAB_TO_GROUP = Map.of(
            "admin",    "ADMIN_POLICY",
            "user",     "USER_POLICY",
            "password", "PASSWORD_POLICY",
            "login",    "LOGIN_POLICY",
            "account",  "ACCOUNT_POLICY",
            "audit",    "AUDIT_POLICY",
            "system",   "SYSTEM_POLICY"
    );

    private final SystemPolicyService systemPolicyService;

    /**
     * GET /policy/manage/{tab}
     */
    @GetMapping("/{tab}")
    public String view(@PathVariable String tab, Model model) {
        if (!VALID_TABS.contains(tab)) {
            return "redirect:/policy/manage/admin";
        }

        model.addAttribute("activeTab", tab);

        if ("history".equals(tab)) {
            List<SystemPolicyHistory> histList = systemPolicyService.getRecentHistory(100);
            model.addAttribute("historyList", histList);
        } else {
            String group = TAB_TO_GROUP.get(tab);
            List<SystemPolicy> policies = systemPolicyService.getPolicies(group);
            Map<String, String> policyMap = new LinkedHashMap<>();
            for (SystemPolicy p : policies) {
                policyMap.put(p.getPolicyKey(), p.getPolicyValue());
            }
            model.addAttribute("policyMap", policyMap);
            model.addAttribute("policies", policies);
            model.addAttribute("policyGroup", group);
        }

        return "main/policy/manage";
    }

    /**
     * POST /policy/manage/{tab}/save
     */
    @PostMapping("/{tab}/save")
    public String save(
            @PathVariable String tab,
            @RequestParam MultiValueMap<String, String> params,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        if (!VALID_TABS.contains(tab) || "history".equals(tab)) {
            return "redirect:/policy/manage/admin";
        }

        String group = TAB_TO_GROUP.get(tab);
        String changedBy = (principal != null) ? principal.getUsername() : "SYSTEM";

        Map<String, String> updates = new LinkedHashMap<>();
        List<SystemPolicy> policies = systemPolicyService.getPolicies(group);

        for (SystemPolicy policy : policies) {
            String key = policy.getPolicyKey();
            if ("BOOLEAN".equals(policy.getValueType())) {
                // hidden(false) + checkbox(true) 패턴: 마지막 값이 실제 상태
                // 체크됨 → ["false","true"] → 마지막="true"
                // 미체크 → ["false"]       → 마지막="false"
                List<String> vals = params.get(key);
                String lastVal = (vals != null && !vals.isEmpty()) ? vals.get(vals.size() - 1) : "false";
                updates.put(key, "true".equals(lastVal) ? "true" : "false");
            } else {
                if (params.containsKey(key)) {
                    updates.put(key, params.getFirst(key));
                }
            }
        }

        try {
            systemPolicyService.saveAll(group, updates, changedBy);
            redirectAttributes.addFlashAttribute("successMsg", "정책이 저장되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/policy/manage/" + tab;
    }
}
