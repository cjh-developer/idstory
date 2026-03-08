package com.idstory.sso.controller;

import com.idstory.client.service.ClientService;
import com.idstory.sso.dto.SsoClientCreateDto;
import com.idstory.sso.dto.SsoClientUpdateDto;
import com.idstory.sso.entity.SsoClient;
import com.idstory.sso.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SSO 관리 페이지 컨트롤러
 * - /sso/clients  : SSO 클라이언트 설정
 * - /sso/tokens   : 토큰 현황
 * - /sso/auth-codes : 인증코드 이력
 * - /sso/access-log : 접근 이력
 */
@Controller
@RequestMapping("/sso")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SsoManageController {

    private static final int PAGE_SIZE = 20;

    private final SsoClientService    ssoClientService;
    private final SsoAuthCodeService  authCodeService;
    private final SsoTokenService     tokenService;
    private final SsoAccessLogService accessLogService;
    private final ClientService       clientService;

    // ── SSO 클라이언트 목록 페이지 ────────────────────────────────────────────

    @GetMapping("/clients")
    public String clients(Model model) {
        List<SsoClient> ssoClients = ssoClientService.findAll();

        // clientOid → clientName 맵 (ids_iam_client 참조)
        Map<String, String> clientNames = clientService.findAll().stream()
                .collect(Collectors.toMap(c -> c.getClientOid(), c -> c.getClientName()));

        model.addAttribute("ssoClients",  ssoClients);
        model.addAttribute("clientNames", clientNames);
        model.addAttribute("allClients",  clientService.findAll());
        return "main/sso/clients";
    }

    // ── SSO 클라이언트 단건 조회 API ─────────────────────────────────────────

    @GetMapping("/api/clients/{ssoClientOid}")
    @ResponseBody
    public ResponseEntity<?> getClient(@PathVariable String ssoClientOid) {
        try {
            SsoClient c = ssoClientService.findBySsoClientOid(ssoClientOid);
            return ResponseEntity.ok(ssoClientService.toMap(c));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── SSO 클라이언트 등록 ───────────────────────────────────────────────────

    @PostMapping("/clients/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody SsoClientCreateDto dto,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            String rawSecret = ssoClientService.create(dto, principal.getUsername());
            return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "message",   "SSO 클라이언트가 등록되었습니다.",
                    "rawSecret", rawSecret
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── SSO 클라이언트 수정 ───────────────────────────────────────────────────

    @PostMapping("/clients/{ssoClientOid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String ssoClientOid,
            @Valid @RequestBody SsoClientUpdateDto dto,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            ssoClientService.update(ssoClientOid, dto, principal.getUsername());
            return ResponseEntity.ok(Map.of("success", true, "message", "수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── SSO 클라이언트 삭제 ───────────────────────────────────────────────────

    @PostMapping("/clients/{ssoClientOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String ssoClientOid,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            ssoClientService.delete(ssoClientOid, principal.getUsername());
            return ResponseEntity.ok(Map.of("success", true, "message", "삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── Secret 재발급 ────────────────────────────────────────────────────────

    @PostMapping("/clients/{ssoClientOid}/regenerate-secret")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> regenerateSecret(
            @PathVariable String ssoClientOid,
            @AuthenticationPrincipal UserDetails principal) {
        try {
            String rawSecret = ssoClientService.regenerateSecret(ssoClientOid, principal.getUsername());
            return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "message",   "Secret이 재발급되었습니다.",
                    "rawSecret", rawSecret
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 토큰 현황 페이지 ──────────────────────────────────────────────────────

    @GetMapping("/tokens")
    public String tokens(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String tokenType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to   = dateTo   != null ? dateTo.atTime(23, 59, 59) : null;

        model.addAttribute("tokenPage",  tokenService.findByFilter(clientId, userId, tokenType, from, to,
                PageRequest.of(page, PAGE_SIZE)));
        model.addAttribute("ssoClients", ssoClientService.findAll());
        model.addAttribute("clientId",   clientId);
        model.addAttribute("userId",     userId);
        model.addAttribute("tokenType",  tokenType);
        model.addAttribute("dateFrom",   dateFrom);
        model.addAttribute("dateTo",     dateTo);
        return "main/sso/tokens";
    }

    // ── 토큰 취소 ──────────────────────────────────────────────────────────────

    @PostMapping("/tokens/{tokenOid}/revoke")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revokeToken(
            @PathVariable String tokenOid) {
        try {
            tokenService.revoke(tokenOid);
            return ResponseEntity.ok(Map.of("success", true, "message", "토큰이 취소되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 인증코드 이력 페이지 ──────────────────────────────────────────────────

    @GetMapping("/auth-codes")
    public String authCodes(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to   = dateTo   != null ? dateTo.atTime(23, 59, 59) : null;

        model.addAttribute("codePage",   authCodeService.findByFilter(clientId, userId, status, from, to,
                PageRequest.of(page, PAGE_SIZE)));
        model.addAttribute("ssoClients", ssoClientService.findAll());
        model.addAttribute("clientId",   clientId);
        model.addAttribute("userId",     userId);
        model.addAttribute("status",     status);
        model.addAttribute("dateFrom",   dateFrom);
        model.addAttribute("dateTo",     dateTo);
        return "main/sso/auth-codes";
    }

    // ── 접근 이력 페이지 ──────────────────────────────────────────────────────

    @GetMapping("/access-log")
    public String accessLog(
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to   = dateTo   != null ? dateTo.atTime(23, 59, 59) : null;

        model.addAttribute("logPage",    accessLogService.findByFilter(clientId, userId, action, status, ipAddress,
                from, to, PageRequest.of(page, PAGE_SIZE)));
        model.addAttribute("ssoClients", ssoClientService.findAll());
        model.addAttribute("clientId",   clientId);
        model.addAttribute("userId",     userId);
        model.addAttribute("action",     action);
        model.addAttribute("status",     status);
        model.addAttribute("ipAddress",  ipAddress);
        model.addAttribute("dateFrom",   dateFrom);
        model.addAttribute("dateTo",     dateTo);
        return "main/sso/access-log";
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return request.getRemoteAddr();
    }
}
