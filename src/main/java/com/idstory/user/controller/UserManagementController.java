package com.idstory.user.controller;

import com.idstory.dept.entity.Department;
import com.idstory.dept.service.DepartmentService;
import com.idstory.user.dto.UserCreateDto;
import com.idstory.user.dto.UserUpdateDto;
import com.idstory.user.entity.SysUser;
import com.idstory.user.service.SysUserService;
import com.idstory.userorgmap.entity.UserOrgMap;
import com.idstory.userorgmap.service.UserOrgMapService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자 관리 컨트롤러
 *
 * <ul>
 *   <li>GET  /user/list          — 사용자 목록 페이지</li>
 *   <li>GET  /user/api/{oid}     — 수정 모달용 단건 JSON</li>
 *   <li>POST /user/api/register  — 등록 모달 AJAX 처리</li>
 *   <li>POST /user/api/{oid}/update — 수정 모달 AJAX 처리</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/user")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserManagementController {

    private final SysUserService sysUserService;
    private final DepartmentService departmentService;
    private final UserOrgMapService userOrgMapService;

    private static final int PAGE_SIZE = 20;

    // ─────────────────────────────────────────────────────────────
    //  페이지 렌더링
    // ─────────────────────────────────────────────────────────────

    /** GET /user/list — 사용자 목록 */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String useYn,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String lockYn,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        var userPage = sysUserService.findUsers(keyword, deptCode, role, useYn, status, lockYn,
                PageRequest.of(page, PAGE_SIZE));

        Map<String, String> deptMap = departmentService.findEnabledDepartments()
                .stream()
                .collect(Collectors.toMap(Department::getDeptCode, Department::getDeptName));

        model.addAttribute("userPage", userPage);
        model.addAttribute("deptMap",  deptMap);
        model.addAttribute("deptList", departmentService.findEnabledDepartments());
        model.addAttribute("keyword",  keyword);
        model.addAttribute("deptCode", deptCode);
        model.addAttribute("role",     role);
        model.addAttribute("useYn",    useYn);
        model.addAttribute("status",   status);
        model.addAttribute("lockYn",   lockYn);
        return "main/user/list";
    }

    // ─────────────────────────────────────────────────────────────
    //  모달 AJAX API
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /user/api/{oid} — 수정 모달 초기 데이터 (JSON)
     */
    @GetMapping("/api/{oid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiGetUser(@PathVariable String oid) {
        try {
            SysUser user = sysUserService.getUserByOid(oid);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("oid",            user.getOid());
            data.put("userId",         user.getUserId());
            data.put("name",           user.getName());
            data.put("email",          user.getEmail());
            data.put("phone",          user.getPhone());
            data.put("deptCode",       user.getDeptCode());
            data.put("role",           user.getRole());
            data.put("useYn",          user.getUseYn());
            data.put("status",         user.getStatus());
            data.put("lockYn",         user.getLockYn());
            data.put("loginFailCount", user.getLoginFailCount());
            data.put("concurrentYn",  user.getConcurrentYn());
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
     * POST /user/api/register — 등록 모달 AJAX 처리 (JSON 응답)
     */
    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiRegister(
            @Valid @ModelAttribute UserCreateDto dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getFieldErrors().stream()
                    .map(fe -> fe.getDefaultMessage())
                    .findFirst().orElse("입력 값을 확인하세요.");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", firstError));
        }

        try {
            String performedBy = (principal != null) ? principal.getUsername() : "SYSTEM";
            String ip = getClientIp(request);
            SysUser user = sysUserService.createUser(dto, performedBy, ip);

            // 조직 정보 등록 (직위/직급/직책 중 하나라도 입력된 경우)
            if (hasOrgInfo(dto)) {
                String deptOid = null, deptName = null;
                if (dto.getDeptCode() != null && !dto.getDeptCode().isBlank()) {
                    Department dept = departmentService.findByDeptCode(dto.getDeptCode()).orElse(null);
                    if (dept != null) {
                        deptOid = dept.getDeptOid();
                        deptName = dept.getDeptName();
                    }
                }
                userOrgMapService.addOrgMap(
                        user.getOid(), user.getUserId(), user.getName(),
                        deptOid, deptName,
                        dto.getPositionOid(), dto.getPositionName(),
                        dto.getGradeOid(),    dto.getGradeName(),
                        dto.getCompRoleOid(), dto.getCompRoleName(),
                        "Y", performedBy);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "사용자 '" + user.getUserId() + "'이(가) 등록되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /user/api/{oid}/update — 수정 모달 AJAX 처리 (JSON 응답)
     */
    @PostMapping("/api/{oid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiUpdateUser(
            @PathVariable String oid,
            @Valid @ModelAttribute UserUpdateDto dto,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getFieldErrors().stream()
                    .map(fe -> fe.getDefaultMessage())
                    .findFirst().orElse("입력 값을 확인하세요.");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", firstError));
        }

        try {
            String performedBy = (principal != null) ? principal.getUsername() : "SYSTEM";
            String ip = getClientIp(request);
            sysUserService.updateUser(oid, dto, performedBy, ip);
            return ResponseEntity.ok(Map.of("success", true, "message", "사용자 정보가 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  조직 매핑 API
    // ─────────────────────────────────────────────────────────────

    /** GET /user/api/{userOid}/org-maps — 사용자 조직 매핑 목록 */
    @GetMapping("/api/{userOid}/org-maps")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> apiGetOrgMaps(@PathVariable String userOid) {
        List<UserOrgMap> maps = userOrgMapService.findByUserOid(userOid);
        return ResponseEntity.ok(maps.stream().map(userOrgMapService::toMap).toList());
    }

    /** POST /user/api/{userOid}/org-maps — 조직 매핑 추가 */
    @PostMapping("/api/{userOid}/org-maps")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiAddOrgMap(
            @PathVariable String userOid,
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) String positionOid,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) String gradeOid,
            @RequestParam(required = false) String gradeName,
            @RequestParam(required = false) String compRoleOid,
            @RequestParam(required = false) String compRoleName,
            @RequestParam(defaultValue = "Y") String isPrimary,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            SysUser user = sysUserService.getUserByOid(userOid);
            String deptOid = null, deptName = null;
            if (deptCode != null && !deptCode.isBlank()) {
                Department dept = departmentService.findByDeptCode(deptCode).orElse(null);
                if (dept != null) {
                    deptOid = dept.getDeptOid();
                    deptName = dept.getDeptName();
                }
            }
            String performedBy = (principal != null) ? principal.getUsername() : "SYSTEM";
            userOrgMapService.addOrgMap(
                    user.getOid(), user.getUserId(), user.getName(),
                    deptOid, deptName,
                    positionOid, positionName,
                    gradeOid,    gradeName,
                    compRoleOid, compRoleName,
                    isPrimary,   performedBy);
            return ResponseEntity.ok(Map.of("success", true, "message", "조직 정보가 추가되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** POST /user/api/org-maps/{mapOid}/delete — 조직 매핑 삭제 */
    @PostMapping("/api/org-maps/{mapOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiDeleteOrgMap(
            @PathVariable String mapOid,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            userOrgMapService.deleteByMapOid(mapOid);
            return ResponseEntity.ok(Map.of("success", true, "message", "조직 정보가 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    private boolean hasOrgInfo(UserCreateDto dto) {
        return (dto.getPositionOid() != null && !dto.getPositionOid().isBlank())
                || (dto.getGradeOid()    != null && !dto.getGradeOid().isBlank())
                || (dto.getCompRoleOid() != null && !dto.getCompRoleOid().isBlank());
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
