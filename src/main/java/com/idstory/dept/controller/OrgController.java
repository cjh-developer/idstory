package com.idstory.dept.controller;

import com.idstory.dept.dto.DeptCreateDto;
import com.idstory.dept.dto.DeptUpdateDto;
import com.idstory.dept.entity.Department;
import com.idstory.dept.service.DepartmentService;
import com.idstory.depthead.entity.DeptHead;
import com.idstory.depthead.service.DeptHeadService;
import com.idstory.user.entity.SysUser;
import com.idstory.user.service.SysUserService;
import com.idstory.userorgmap.entity.UserOrgMap;
import com.idstory.userorgmap.service.UserOrgMapService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 조직도 관리 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OrgController {

    private final DepartmentService departmentService;
    private final SysUserService sysUserService;
    private final UserOrgMapService userOrgMapService;
    private final DeptHeadService deptHeadService;

    // ──────────────────────────────────────────────────────────────────────────
    // 페이지
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 조직도 관리 페이지
     */
    @GetMapping("/org/chart")
    public String chartPage(Model model) {
        return "main/org/chart";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API — 목록
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 부서 목록 조회 (JSON)
     *
     * @param includeDeleted 삭제된 부서 포함 여부 (기본값 false)
     */
    @GetMapping("/org/api/depts")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDepts(
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        List<Department> depts = departmentService.findAllForChart(includeDeleted);
        List<Map<String, Object>> result = depts.stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * 부서 단건 조회 (JSON)
     */
    @GetMapping("/org/api/depts/{deptOid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDept(@PathVariable String deptOid) {
        Department dept = departmentService.getDeptByOid(deptOid);
        return ResponseEntity.ok(toMap(dept));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API — 등록
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 부서 등록
     */
    @PostMapping("/org/api/depts/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerDept(
            @Valid @ModelAttribute DeptCreateDto dto,
            BindingResult br,
            Authentication auth,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }

        try {
            String performedBy = auth.getName();
            Department dept = departmentService.createDept(dto, performedBy);
            result.put("success", true);
            result.put("message", "부서가 등록되었습니다.");
            result.put("deptOid", dept.getDeptOid());
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API — 수정
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 부서 수정
     */
    @PostMapping("/org/api/depts/{deptOid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateDept(
            @PathVariable String deptOid,
            @Valid @ModelAttribute DeptUpdateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }

        try {
            departmentService.updateDept(deptOid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "부서 정보가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API — 삭제 / 복원
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 부서 소프트 삭제
     */
    @PostMapping("/org/api/depts/{deptOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteDept(
            @PathVariable String deptOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            departmentService.softDeleteDept(deptOid, auth.getName());
            result.put("success", true);
            result.put("message", "부서가 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 소프트 삭제된 부서 복원
     */
    @PostMapping("/org/api/depts/{deptOid}/restore")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreDept(
            @PathVariable String deptOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            departmentService.restoreDept(deptOid, auth.getName());
            result.put("success", true);
            result.put("message", "부서가 복원되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 부서장 관리 — 페이지
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/org/dept-head")
    public String deptHeadPage(Model model) {
        return "main/org/dept-head";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 부서장 관리 — API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 현재 부서장 조회
     */
    @GetMapping("/org/api/dept-head")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDeptHead(@RequestParam String deptOid) {
        Map<String, Object> result = new LinkedHashMap<>();
        deptHeadService.findByDeptOid(deptOid).ifPresent(h -> {
            result.put("headOid",  h.getHeadOid());
            result.put("deptOid",  h.getDeptOid());
            result.put("deptName", h.getDeptName());
            result.put("userOid",  h.getUserOid());
            result.put("userId",   h.getUserId());
            result.put("userName", h.getUserName());
            result.put("createdAt", h.getCreatedAt() != null ? h.getCreatedAt().toString() : null);
            result.put("createdBy", h.getCreatedBy());
        });
        return ResponseEntity.ok(result);
    }

    /**
     * 부서장 등록/교체
     */
    @PostMapping("/org/api/dept-head")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignDeptHead(
            @RequestParam String deptOid,
            @RequestParam String userOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Department dept = departmentService.getDeptByOid(deptOid);
            SysUser user = sysUserService.getUserByOid(userOid);
            deptHeadService.assignHead(
                    deptOid, dept.getDeptName(),
                    userOid, user.getUserId(), user.getName(),
                    auth.getName());
            result.put("success", true);
            result.put("message", "부서장이 등록되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 부서장 해제
     */
    @PostMapping("/org/api/dept-head/{headOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeDeptHead(@PathVariable String headOid) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            deptHeadService.removeHead(headOid);
            result.put("success", true);
            result.put("message", "부서장이 해제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 조직사용자 — 페이지
    // ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/org/users")
    public String usersPage(Model model) {
        return "main/org/users";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 조직사용자 — API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 선택된 부서의 사용자 목록 조회 (JSON, 페이징)
     *
     * @param deptOid 부서 OID
     * @param keyword 아이디/이름 검색어
     * @param page    페이지 번호 (0부터)
     */
    @GetMapping("/org/api/dept-users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDeptUsers(
            @RequestParam String deptOid,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {

        final int PAGE_SIZE = 10;
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Department dept = departmentService.getDeptByOid(deptOid);
            Page<SysUser> userPage = sysUserService.findUsersByDept(
                    dept.getDeptCode(), keyword, PageRequest.of(page, PAGE_SIZE));

            // 겸직 사용자: ids_iam_user_org_map에서 is_primary='N' 으로 이 부서에 등록된 사용자
            List<UserOrgMap> concMaps = userOrgMapService.findConcurrentByDeptOid(dept.getDeptOid());
            List<Map<String, Object>> concurrentUsers = concMaps.stream()
                    .map(m -> {
                        try {
                            SysUser u = sysUserService.getUserByOid(m.getUserOid());
                            Map<String, Object> d = userToMap(u);
                            d.put("mapOid", m.getMapOid());
                            return d;
                        } catch (Exception e) {
                            log.warn("[OrgController] 겸직 사용자 조회 실패 - userOid: {}", m.getUserOid());
                            return null;
                        }
                    })
                    .filter(d -> d != null)
                    .toList();

            result.put("deptOid",        dept.getDeptOid());
            result.put("deptCode",       dept.getDeptCode());
            result.put("deptName",       dept.getDeptName());
            result.put("users",          userPage.getContent().stream().map(this::userToMap).toList());
            result.put("totalElements",  userPage.getTotalElements());
            result.put("totalPages",     userPage.getTotalPages());
            result.put("currentPage",    userPage.getNumber());
            result.put("pageSize",       PAGE_SIZE);
            result.put("concurrentUsers", concurrentUsers);
        } catch (IllegalArgumentException e) {
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 배정 모달용: 전체 활성 사용자 검색 (JSON, 페이징)
     */
    @GetMapping("/org/api/assignable-users")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAssignableUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {

        final int PAGE_SIZE = 10;
        Page<SysUser> userPage = sysUserService.searchAssignableUsers(
                keyword, PageRequest.of(page, PAGE_SIZE));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users",         userPage.getContent().stream().map(this::userToMap).toList());
        result.put("totalElements", userPage.getTotalElements());
        result.put("totalPages",    userPage.getTotalPages());
        result.put("currentPage",   userPage.getNumber());
        result.put("pageSize",      PAGE_SIZE);
        return ResponseEntity.ok(result);
    }

    /**
     * 사용자를 부서에 배정 (deptOid → deptCode 변환 후 저장)
     */
    @PostMapping("/org/api/users/{userOid}/assign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignUser(
            @PathVariable String userOid,
            @RequestParam String deptOid,
            Authentication auth,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Department dept = departmentService.getDeptByOid(deptOid);
            sysUserService.assignDept(userOid, dept.getDeptCode(), auth.getName(), getIp(request));
            result.put("success", true);
            result.put("message", "부서에 배정되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 사용자 부서 이동 (targetDeptOid → deptCode 변환 후 저장)
     */
    @PostMapping("/org/api/users/{userOid}/move-dept")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> moveUserDept(
            @PathVariable String userOid,
            @RequestParam String targetDeptOid,
            Authentication auth,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Department dept = departmentService.getDeptByOid(targetDeptOid);
            sysUserService.assignDept(userOid, dept.getDeptCode(), auth.getName(), getIp(request));
            result.put("success", true);
            result.put("message", "부서가 이동되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 사용자 부서 해제 (deptCode → null)
     */
    @PostMapping("/org/api/users/{userOid}/unassign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unassignUser(
            @PathVariable String userOid,
            Authentication auth,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            sysUserService.assignDept(userOid, null, auth.getName(), getIp(request));
            result.put("success", true);
            result.put("message", "부서 배정이 해제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, Object> userToMap(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("oid",      u.getOid());
        m.put("userId",   u.getUserId());
        m.put("name",     u.getName());
        m.put("email",    u.getEmail());
        m.put("phone",    u.getPhone());
        m.put("deptCode", u.getDeptCode());
        m.put("role",     u.getRole());
        m.put("useYn",    u.getUseYn());
        m.put("status",   u.getStatus());
        m.put("lockYn",   u.getLockYn());
        return m;
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private Map<String, Object> toMap(Department d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("deptOid",       d.getDeptOid());
        m.put("deptCode",      d.getDeptCode());
        m.put("deptName",      d.getDeptName());
        m.put("parentDeptOid", d.getParentDeptOid());
        m.put("sortOrder",     d.getSortOrder());
        m.put("useYn",         d.getUseYn());
        m.put("deptType",      d.getDeptType());
        m.put("deptTel",       d.getDeptTel());
        m.put("deptFax",       d.getDeptFax());
        m.put("deptAddress",   d.getDeptAddress());
        m.put("deletedAt",     d.getDeletedAt() != null ? d.getDeletedAt().toString() : null);
        m.put("deletedBy",     d.getDeletedBy());
        m.put("createdAt",     d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
        m.put("createdBy",     d.getCreatedBy());
        m.put("updatedAt",     d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : null);
        m.put("updatedBy",     d.getUpdatedBy());
        return m;
    }
}
