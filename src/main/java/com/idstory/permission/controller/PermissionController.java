package com.idstory.permission.controller;

import com.idstory.client.entity.Client;
import com.idstory.client.service.ClientService;
import com.idstory.permission.dto.PermissionCreateDto;
import com.idstory.permission.dto.PermissionUpdateDto;
import com.idstory.permission.entity.Permission;
import com.idstory.permission.service.PermissionService;
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
 * 권한 관리 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/permission")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;
    private final ClientService     clientService;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping
    public String page(Model model) {
        List<Client> clients = clientService.findAll();
        model.addAttribute("clients", clients);
        return "main/auth/permission";
    }

    // ── API: 트리 조회 ─────────────────────────────────────────────

    @GetMapping("/tree")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> tree(@RequestParam String clientOid) {
        List<Permission> treeNodes = permissionService.buildTree(clientOid);
        return ResponseEntity.ok(permissionService.toTreeMapList(treeNodes));
    }

    // ── API: 단건 조회 ────────────────────────────────────────────

    @GetMapping("/{oid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String oid) {
        try {
            Permission p = permissionService.getByOid(oid);
            return ResponseEntity.ok(permissionService.toMap(p));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── API: 등록 ─────────────────────────────────────────────────

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(
            @Valid @ModelAttribute PermissionCreateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            Permission p = permissionService.create(dto, auth.getName());
            result.put("success", true);
            result.put("message", "권한이 등록되었습니다.");
            result.put("permOid", p.getPermOid());
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
            @Valid @ModelAttribute PermissionUpdateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            permissionService.update(oid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "권한 정보가 수정되었습니다.");
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
            permissionService.softDelete(oid, auth.getName());
            result.put("success", true);
            result.put("message", "권한이 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
