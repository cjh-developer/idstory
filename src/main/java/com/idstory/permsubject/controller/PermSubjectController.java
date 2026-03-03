package com.idstory.permsubject.controller;

import com.idstory.client.entity.Client;
import com.idstory.client.service.ClientService;
import com.idstory.permsubject.service.PermSubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 권한 사용자 컨트롤러 (권한-대상 매핑)
 * 대상 유형: DEPT | USER | GRADE | POSITION | EXCEPTION
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/perm-user")
@PreAuthorize("hasRole('ADMIN')")
public class PermSubjectController {

    private final PermSubjectService permSubjectService;
    private final ClientService      clientService;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping
    public String page(Model model) {
        List<Client> clients = clientService.findAll();
        model.addAttribute("clients", clients);
        return "main/auth/perm-user";
    }

    // ── API: 배정된 대상 목록 (상세 포함) ────────────────────────────

    @GetMapping("/subjects")
    @ResponseBody
    public ResponseEntity<Object> subjects(
            @RequestParam String permOid,
            @RequestParam String type) {
        try {
            return ResponseEntity.ok(permSubjectService.getSubjectsWithDetails(permOid, type));
        } catch (Exception e) {
            log.error("subjects 조회 오류", e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // ── API: 대상 배정 ────────────────────────────────────────────

    @PostMapping("/assign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assign(
            @RequestParam String permOid,
            @RequestParam String subjectType,
            @RequestParam String subjectOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            permSubjectService.assign(permOid, subjectType, subjectOid, auth.getName());
            result.put("success", true);
            result.put("message", "배정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 대상 배정 해제 ───────────────────────────────────────

    @PostMapping("/revoke")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revoke(
            @RequestParam String permSubjectOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            permSubjectService.revoke(permSubjectOid);
            result.put("success", true);
            result.put("message", "배정이 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 유효 사용자 계산 ─────────────────────────────────────

    @GetMapping("/effective")
    @ResponseBody
    public ResponseEntity<Object> effective(@RequestParam String permOid) {
        try {
            return ResponseEntity.ok(permSubjectService.getEffectiveUsers(permOid));
        } catch (Exception e) {
            log.error("유효 사용자 계산 오류", e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
