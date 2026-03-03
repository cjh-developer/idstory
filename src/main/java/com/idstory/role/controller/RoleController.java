package com.idstory.role.controller;

import com.idstory.role.dto.RoleCreateDto;
import com.idstory.role.dto.RoleUpdateDto;
import com.idstory.role.entity.Role;
import com.idstory.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 역할 관리 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/role")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping
    public String page(Model model) {
        return "main/auth/role";
    }

    // ── API: 트리 조회 ─────────────────────────────────────────────

    @GetMapping("/tree")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> tree() {
        List<Role> treeNodes = roleService.buildTree();
        return ResponseEntity.ok(roleService.toTreeMapList(treeNodes));
    }

    // ── API: 단건 조회 ────────────────────────────────────────────

    @GetMapping("/{oid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String oid) {
        try {
            Role r = roleService.getByOid(oid);
            return ResponseEntity.ok(roleService.toMap(r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── API: 등록 ─────────────────────────────────────────────────

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(
            @Valid @ModelAttribute RoleCreateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            Role r = roleService.create(dto, auth.getName());
            result.put("success", true);
            result.put("message", "역할이 등록되었습니다.");
            result.put("roleOid", r.getRoleOid());
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 수정 ─────────────────────────────────────────────────

    @PostMapping("/{oid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String oid,
            @Valid @ModelAttribute RoleUpdateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            roleService.update(oid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "역할 정보가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 삭제 ─────────────────────────────────────────────────

    @PostMapping("/{oid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String oid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            roleService.softDelete(oid, auth.getName());
            result.put("success", true);
            result.put("message", "역할이 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
