package com.idstory.client.controller;

import com.idstory.client.dto.ClientCreateDto;
import com.idstory.client.dto.ClientUpdateDto;
import com.idstory.client.entity.Client;
import com.idstory.client.service.ClientService;
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
}
