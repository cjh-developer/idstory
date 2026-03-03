package com.idstory.permrole.controller;

import com.idstory.client.entity.Client;
import com.idstory.client.service.ClientService;
import com.idstory.permrole.service.PermRoleService;
import com.idstory.permission.service.PermissionService;
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
 * 권한 설정 컨트롤러 (권한-역할 매핑)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/setting")
@PreAuthorize("hasRole('ADMIN')")
public class PermRoleController {

    private final PermRoleService    permRoleService;
    private final ClientService      clientService;
    private final PermissionService  permissionService;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping
    public String page(Model model) {
        List<Client> clients = clientService.findAll();
        model.addAttribute("clients", clients);
        return "main/auth/setting";
    }

    // ── API: 권한의 역할 목록 (배정/미배정) ──────────────────────────

    @GetMapping("/roles")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> roles(@RequestParam String permOid) {
        try {
            Map<String, Object> result = permRoleService.getRolesForPerm(permOid);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // ── API: 역할 배정 ────────────────────────────────────────────

    @PostMapping("/assign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assign(
            @RequestParam String permOid,
            @RequestParam String roleOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            permRoleService.assign(permOid, roleOid, auth.getName());
            result.put("success", true);
            result.put("message", "역할이 배정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 역할 해제 ────────────────────────────────────────────

    @PostMapping("/revoke")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revoke(
            @RequestParam String permOid,
            @RequestParam String roleOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            permRoleService.revoke(permOid, roleOid, auth.getName());
            result.put("success", true);
            result.put("message", "역할 배정이 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
