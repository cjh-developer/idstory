package com.idstory.position.controller;

import com.idstory.position.dto.PositionCreateDto;
import com.idstory.position.dto.PositionUpdateDto;
import com.idstory.position.entity.Position;
import com.idstory.position.service.PositionService;
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
 * 직위 관리 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PositionController {

    private final PositionService positionService;
    private static final int PAGE_SIZE = 10;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping("/org/position")
    public String page(Model model) {
        return "main/org/position";
    }

    // ── API: 목록 조회 ────────────────────────────────────────────

    @GetMapping("/org/api/positions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) String useYn,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page) {

        Page<Position> result = positionService.findPositions(
                positionCode, positionName, useYn, includeDeleted,
                PageRequest.of(page, PAGE_SIZE));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content",       result.getContent().stream().map(positionService::toMap).toList());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages",    result.getTotalPages());
        response.put("currentPage",   result.getNumber());
        response.put("pageSize",      PAGE_SIZE);
        return ResponseEntity.ok(response);
    }

    // ── API: 단건 조회 ────────────────────────────────────────────

    @GetMapping("/org/api/positions/{positionOid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String positionOid) {
        try {
            Position p = positionService.getByOid(positionOid);
            return ResponseEntity.ok(positionService.toMap(p));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── API: 등록 ─────────────────────────────────────────────────

    @PostMapping("/org/api/positions/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @Valid @ModelAttribute PositionCreateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            Position p = positionService.createPosition(dto, auth.getName());
            result.put("success",     true);
            result.put("message",     "직위가 등록되었습니다.");
            result.put("positionOid", p.getPositionOid());
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 수정 ─────────────────────────────────────────────────

    @PostMapping("/org/api/positions/{positionOid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String positionOid,
            @Valid @ModelAttribute PositionUpdateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            positionService.updatePosition(positionOid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "직위 정보가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 삭제 ─────────────────────────────────────────────────

    @PostMapping("/org/api/positions/{positionOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String positionOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            positionService.deletePosition(positionOid, auth.getName());
            result.put("success", true);
            result.put("message", "직위가 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
