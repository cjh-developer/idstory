package com.idstory.comprole.controller;

import com.idstory.comprole.dto.CompRoleCreateDto;
import com.idstory.comprole.dto.CompRoleUpdateDto;
import com.idstory.comprole.entity.CompRole;
import com.idstory.comprole.service.CompRoleService;
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
import java.util.Map;

/**
 * 직책 관리 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CompRoleController {

    private final CompRoleService compRoleService;
    private static final int PAGE_SIZE = 10;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping("/org/comp-role")
    public String page(Model model) {
        return "main/org/comp-role";
    }

    // ── API: 목록 조회 ────────────────────────────────────────────

    @GetMapping("/org/api/comp-roles")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String compRoleCode,
            @RequestParam(required = false) String compRoleName,
            @RequestParam(required = false) String useYn,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page) {

        Page<CompRole> result = compRoleService.findCompRoles(
                compRoleCode, compRoleName, useYn, includeDeleted,
                PageRequest.of(page, PAGE_SIZE));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content",       result.getContent().stream().map(compRoleService::toMap).toList());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages",    result.getTotalPages());
        response.put("currentPage",   result.getNumber());
        response.put("pageSize",      PAGE_SIZE);
        return ResponseEntity.ok(response);
    }

    // ── API: 단건 조회 ────────────────────────────────────────────

    @GetMapping("/org/api/comp-roles/{compRoleOid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String compRoleOid) {
        try {
            CompRole r = compRoleService.getByOid(compRoleOid);
            return ResponseEntity.ok(compRoleService.toMap(r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── API: 등록 ─────────────────────────────────────────────────

    @PostMapping("/org/api/comp-roles/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @Valid @ModelAttribute CompRoleCreateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            CompRole r = compRoleService.createCompRole(dto, auth.getName());
            result.put("success",     true);
            result.put("message",     "직책이 등록되었습니다.");
            result.put("compRoleOid", r.getCompRoleOid());
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 수정 ─────────────────────────────────────────────────

    @PostMapping("/org/api/comp-roles/{compRoleOid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String compRoleOid,
            @Valid @ModelAttribute CompRoleUpdateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            compRoleService.updateCompRole(compRoleOid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "직책 정보가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 삭제 ─────────────────────────────────────────────────

    @PostMapping("/org/api/comp-roles/{compRoleOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String compRoleOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            compRoleService.deleteCompRole(compRoleOid, auth.getName());
            result.put("success", true);
            result.put("message", "직책이 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
