package com.idstory.roleuser.controller;

import com.idstory.role.service.RoleService;
import com.idstory.roleuser.service.RoleSubjectService;
import com.idstory.roleuser.service.RoleUserService;
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
 * 역할 사용자 컨트롤러
 * - /users : 개인 직접 배정 (ids_iam_role_user)
 * - /subjects : 부서/직위/직급/예외 배정 (ids_iam_role_subject)
 * - /effective : 유효 사용자 계산
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/role-user")
@PreAuthorize("hasRole('ADMIN')")
public class RoleUserController {

    private final RoleUserService    roleUserService;
    private final RoleSubjectService roleSubjectService;
    private final RoleService        roleService;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping
    public String page(Model model) {
        return "main/auth/role-user";
    }

    // ══════════════════════════════════════════════════════════════
    //  개인 배정 (ids_iam_role_user)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/users")
    @ResponseBody
    public ResponseEntity<Object> users(@RequestParam String roleOid) {
        try {
            return ResponseEntity.ok(roleUserService.getUsersByRole(roleOid));
        } catch (Exception e) {
            log.error("역할 사용자 목록 조회 오류", e);
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/assign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assign(
            @RequestParam String roleOid,
            @RequestParam String userOid,
            Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            roleUserService.assign(roleOid, userOid, auth.getName());
            result.put("success", true);
            result.put("message", "사용자가 배정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/revoke")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revoke(
            @RequestParam String roleUserOid,
            Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            roleUserService.revoke(roleUserOid);
            result.put("success", true);
            result.put("message", "배정이 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 사용자가 이미 부서 배정으로 해당 역할을 받고 있는지 확인
     * 개인 추가 전 경고 표시용
     */
    @GetMapping("/check-dept")
    @ResponseBody
    public ResponseEntity<Object> checkDeptCoverage(
            @RequestParam String roleOid,
            @RequestParam String userOid) {
        try {
            List<String> deptNames = roleSubjectService.getUserCoveringDeptNames(roleOid, userOid);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("covered",   !deptNames.isEmpty());
            result.put("deptNames", deptNames);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("부서 커버리지 확인 오류", e);
            return badRequest(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  대상 배정 (ids_iam_role_subject: DEPT/POSITION/GRADE/EXCEPTION)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/subjects")
    @ResponseBody
    public ResponseEntity<Object> subjects(
            @RequestParam String roleOid,
            @RequestParam String type) {
        try {
            return ResponseEntity.ok(roleSubjectService.getSubjectsWithDetails(roleOid, type));
        } catch (Exception e) {
            log.error("대상 목록 조회 오류", e);
            return badRequest(e.getMessage());
        }
    }

    /** 부서 다중 배정 */
    @PostMapping("/subjects/assign-depts")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignDepts(
            @RequestParam String          roleOid,
            @RequestParam List<String>    deptOids,
            @RequestParam(defaultValue = "N") String includeChildren,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<String> assigned = roleSubjectService.assignDepts(roleOid, deptOids, includeChildren, auth.getName());
            result.put("success", true);
            result.put("message", assigned.isEmpty()
                    ? "모두 이미 배정된 항목입니다."
                    : assigned.size() + "개 부서가 배정되었습니다.");
            result.put("assignedCount", assigned.size());
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /** 단건 배정 (POSITION / GRADE / EXCEPTION) */
    @PostMapping("/subjects/assign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignSubject(
            @RequestParam String roleOid,
            @RequestParam String subjectType,
            @RequestParam String subjectOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            roleSubjectService.assign(roleOid, subjectType, subjectOid, auth.getName());
            result.put("success", true);
            result.put("message", "배정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /** 대상 해제 */
    @PostMapping("/subjects/revoke")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revokeSubject(
            @RequestParam String roleSubjectOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            roleSubjectService.revoke(roleSubjectOid);
            result.put("success", true);
            result.put("message", "배정이 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════════════════════
    //  유효 사용자 계산
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/effective")
    @ResponseBody
    public ResponseEntity<Object> effective(@RequestParam String roleOid) {
        try {
            return ResponseEntity.ok(roleSubjectService.getEffectiveUsers(roleOid));
        } catch (Exception e) {
            log.error("유효 사용자 계산 오류", e);
            return badRequest(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    private ResponseEntity<Object> badRequest(String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("message", message);
        return ResponseEntity.badRequest().body(err);
    }
}
