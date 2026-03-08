package com.idstory.client.controller;

import com.idstory.client.dto.ClientCreateDto;
import com.idstory.client.dto.ClientUpdateDto;
import com.idstory.client.entity.Client;
import com.idstory.client.service.ClientService;
import com.idstory.sso.dto.SsoClientUpdateDto;
import com.idstory.sso.service.SsoClientService;
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
 * 클라이언트 관리 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/client")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private final ClientService clientService;
    private final SsoClientService ssoClientService;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping
    public String page(Model model) {
        return "main/auth/client";
    }

    // ── API: 트리 조회 ─────────────────────────────────────────────

    @GetMapping("/tree")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> tree() {
        List<Client> treeNodes = clientService.buildTree();
        return ResponseEntity.ok(clientService.toTreeMapList(treeNodes));
    }

    // ── API: 단건 조회 ────────────────────────────────────────────

    @GetMapping("/{oid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String oid) {
        try {
            Client c = clientService.getByOid(oid);
            return ResponseEntity.ok(clientService.toMap(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── API: 등록 ─────────────────────────────────────────────────

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(
            @Valid @ModelAttribute ClientCreateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            Client c = clientService.create(dto, auth.getName());
            result.put("success",   true);
            result.put("message",   "클라이언트가 등록되었습니다.");
            result.put("clientOid", c.getClientOid());
            result.put("appType",   c.getAppType());

            // SSO 유형이면 SsoClient 자동 생성
            if ("SSO".equals(c.getAppType()) && dto.getSsoClientId() != null && !dto.getSsoClientId().isBlank()) {
                try {
                    String rawSecret = ssoClientService.createFromClient(
                            c.getClientOid(),
                            dto.getSsoClientId(),
                            dto.getAuthUri(),
                            dto.getAuthResult(),
                            dto.getNoUseSso(),
                            dto.getSsoRedirectUris(),
                            "openid profile email",
                            auth.getName());
                    result.put("rawSecret", rawSecret);
                } catch (Exception ex) {
                    log.warn("[ClientController] SSO 설정 등록 실패 (클라이언트는 생성됨): {}", ex.getMessage());
                    result.put("ssoWarning", "클라이언트는 등록되었으나 SSO 설정 등록 중 오류가 발생했습니다: " + ex.getMessage());
                }
            }
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
            @Valid @ModelAttribute ClientUpdateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            clientService.update(oid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "클라이언트 정보가 수정되었습니다.");
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
            clientService.softDelete(oid, auth.getName());
            result.put("success", true);
            result.put("message", "클라이언트가 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: SSO 설정 조회 ──────────────────────────────────────

    @GetMapping("/{oid}/sso")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSsoSettings(@PathVariable String oid) {
        return ssoClientService.findByClientOid(oid)
                .map(sc -> ResponseEntity.ok(ssoClientService.toDetailMap(sc)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── API: SSO 설정 수정 ──────────────────────────────────────

    @PostMapping("/{oid}/sso/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSsoSettings(
            @PathVariable String oid,
            @RequestBody SsoClientUpdateDto dto,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ssoClientService.updateByClientOid(oid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "SSO 설정이 수정되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: SSO Secret 재발급 ─────────────────────────────────

    @PostMapping("/{oid}/sso/regenerate-secret")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> regenerateSsoSecret(
            @PathVariable String oid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String rawSecret = ssoClientService.regenerateSecretByClientOid(oid, auth.getName());
            result.put("success",   true);
            result.put("message",   "Secret이 재발급되었습니다.");
            result.put("rawSecret", rawSecret);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
