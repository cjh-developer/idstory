package com.idstory.admin.controller;

import com.idstory.admin.entity.SysAdmin;
import com.idstory.admin.service.AdminService;
import com.idstory.dept.entity.Department;
import com.idstory.dept.service.DepartmentService;
import com.idstory.user.dto.UserUpdateDto;
import com.idstory.user.entity.SysUser;
import com.idstory.user.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자 관리 컨트롤러
 *
 * <ul>
 *   <li>GET  /admin/list                    — 관리자 목록 페이지</li>
 *   <li>GET  /admin/api/search-users        — 등록 모달 사용자 검색 (JSON)</li>
 *   <li>GET  /admin/api/{adminOid}          — 수정 모달 초기 데이터 (JSON)</li>
 *   <li>POST /admin/api/register            — 관리자 등록 (기존 사용자 승격)</li>
 *   <li>POST /admin/api/{adminOid}/update   — 관리자 수정</li>
 *   <li>POST /admin/api/{adminOid}/demote   — 관리자 해제 (USER 강등)</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminManagementController {

    private final AdminService adminService;
    private final SysUserService sysUserService;
    private final DepartmentService departmentService;

    private static final int PAGE_SIZE = 20;

    // ─────────────────────────────────────────────────────────────
    //  페이지 렌더링
    // ─────────────────────────────────────────────────────────────

    /** GET /admin/list — 관리자 목록 */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        var adminPage = adminService.findAdmins(keyword, PageRequest.of(page, PAGE_SIZE));

        Map<String, String> deptMap = departmentService.findEnabledDepartments()
                .stream()
                .collect(Collectors.toMap(Department::getDeptCode, Department::getDeptName));

        model.addAttribute("adminPage", adminPage);
        model.addAttribute("deptMap",   deptMap);
        model.addAttribute("deptList",  departmentService.findEnabledDepartments());
        model.addAttribute("keyword",   keyword);
        return "main/admin/list";
    }

    // ─────────────────────────────────────────────────────────────
    //  모달 AJAX API
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /admin/api/search-users — 등록 모달 사용자 검색 (role=USER만 반환)
     */
    @GetMapping("/api/search-users")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam(required = false) String keyword) {

        Map<String, String> deptMap = departmentService.findEnabledDepartments()
                .stream()
                .collect(Collectors.toMap(Department::getDeptCode, Department::getDeptName));

        List<Map<String, Object>> result = sysUserService.searchNonAdminUsers(keyword)
                .stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("oid",      u.getOid());
                    m.put("userId",   u.getUserId());
                    m.put("name",     u.getName());
                    m.put("email",    u.getEmail());
                    m.put("phone",    u.getPhone());
                    m.put("deptCode", u.getDeptCode());
                    m.put("deptName", u.getDeptCode() != null
                            ? deptMap.getOrDefault(u.getDeptCode(), u.getDeptCode()) : null);
                    m.put("useYn",    u.getUseYn());
                    m.put("status",   u.getStatus());
                    m.put("validStartDate",
                            u.getValidStartDate() != null ? u.getValidStartDate().toString() : null);
                    m.put("validEndDate",
                            u.getValidEndDate() != null ? u.getValidEndDate().toString() : null);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /admin/api/{adminOid} — 수정 모달 초기 데이터 (JSON)
     */
    @GetMapping("/api/{adminOid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAdmin(@PathVariable String adminOid) {
        try {
            SysAdmin admin = adminService.getAdminByOid(adminOid);
            SysUser  user  = admin.getUser();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("adminOid",       admin.getAdminOid());
            data.put("adminNote",      admin.getAdminNote());
            data.put("grantedAt",      admin.getGrantedAt() != null ? admin.getGrantedAt().toString() : null);
            data.put("grantedBy",      admin.getGrantedBy());
            data.put("oid",            user.getOid());
            data.put("userId",         user.getUserId());
            data.put("name",           user.getName());
            data.put("email",          user.getEmail());
            data.put("phone",          user.getPhone());
            data.put("deptCode",       user.getDeptCode());
            data.put("useYn",          user.getUseYn());
            data.put("status",         user.getStatus());
            data.put("lockYn",         user.getLockYn());
            data.put("loginFailCount", user.getLoginFailCount());
            data.put("validStartDate",
                    user.getValidStartDate() != null ? user.getValidStartDate().toString() : null);
            data.put("validEndDate",
                    user.getValidEndDate() != null ? user.getValidEndDate().toString() : null);
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /admin/api/register — 관리자 등록 (기존 사용자를 ADMIN으로 승격)
     */
    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @RequestParam(required = false) String targetOid,
            @RequestParam(required = false) String adminNote,
            @Valid @ModelAttribute UserUpdateDto dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        if (targetOid == null || targetOid.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "등록할 사용자를 선택하세요."));
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", firstError(bindingResult)));
        }

        try {
            String performedBy = (principal != null) ? principal.getUsername() : "SYSTEM";
            String ip = getClientIp(request);
            SysAdmin admin = adminService.registerAdmin(targetOid, adminNote, performedBy, ip);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "'" + admin.getUser().getUserId() + "'이(가) 관리자로 등록되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /admin/api/{adminOid}/update — 관리자 수정
     */
    @PostMapping("/api/{adminOid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String adminOid,
            @RequestParam(required = false) String adminNote,
            @Valid @ModelAttribute UserUpdateDto dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", firstError(bindingResult)));
        }

        try {
            String performedBy = (principal != null) ? principal.getUsername() : "SYSTEM";
            String ip = getClientIp(request);
            adminService.updateAdmin(adminOid, dto, adminNote, performedBy, ip);
            return ResponseEntity.ok(Map.of("success", true, "message", "관리자 정보가 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /admin/api/{adminOid}/demote — 관리자 해제 (ADMIN → USER)
     */
    @PostMapping("/api/{adminOid}/demote")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> demote(
            @PathVariable String adminOid,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        try {
            String performedBy = (principal != null) ? principal.getUsername() : "SYSTEM";
            String ip = getClientIp(request);
            SysUser user = adminService.demoteAdmin(adminOid, performedBy, ip);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "'" + user.getUserId() + "'의 관리자 권한이 해제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    private String firstError(BindingResult br) {
        return br.getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .findFirst()
                .orElse("입력 값을 확인하세요.");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
