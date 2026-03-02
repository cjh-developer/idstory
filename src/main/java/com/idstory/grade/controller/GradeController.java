package com.idstory.grade.controller;

import com.idstory.grade.dto.GradeCreateDto;
import com.idstory.grade.dto.GradeUpdateDto;
import com.idstory.grade.entity.Grade;
import com.idstory.grade.service.GradeService;
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
 * 직급 관리 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GradeController {

    private final GradeService gradeService;
    private static final int PAGE_SIZE = 10;

    // ── 페이지 ────────────────────────────────────────────────────

    @GetMapping("/org/grade")
    public String page(Model model) {
        return "main/org/grade";
    }

    // ── API: 목록 조회 ────────────────────────────────────────────

    @GetMapping("/org/api/grades")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String gradeCode,
            @RequestParam(required = false) String gradeName,
            @RequestParam(required = false) String useYn,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page) {

        Page<Grade> result = gradeService.findGrades(
                gradeCode, gradeName, useYn, includeDeleted,
                PageRequest.of(page, PAGE_SIZE));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content",       result.getContent().stream().map(gradeService::toMap).toList());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages",    result.getTotalPages());
        response.put("currentPage",   result.getNumber());
        response.put("pageSize",      PAGE_SIZE);
        return ResponseEntity.ok(response);
    }

    // ── API: 단건 조회 ────────────────────────────────────────────

    @GetMapping("/org/api/grades/{gradeOid}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String gradeOid) {
        try {
            Grade g = gradeService.getByOid(gradeOid);
            return ResponseEntity.ok(gradeService.toMap(g));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── API: 등록 ─────────────────────────────────────────────────

    @PostMapping("/org/api/grades/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @Valid @ModelAttribute GradeCreateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            Grade g = gradeService.createGrade(dto, auth.getName());
            result.put("success",  true);
            result.put("message",  "직급이 등록되었습니다.");
            result.put("gradeOid", g.getGradeOid());
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 수정 ─────────────────────────────────────────────────

    @PostMapping("/org/api/grades/{gradeOid}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String gradeOid,
            @Valid @ModelAttribute GradeUpdateDto dto,
            BindingResult br,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            gradeService.updateGrade(gradeOid, dto, auth.getName());
            result.put("success", true);
            result.put("message", "직급 정보가 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ── API: 삭제 ─────────────────────────────────────────────────

    @PostMapping("/org/api/grades/{gradeOid}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String gradeOid,
            Authentication auth) {

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            gradeService.deleteGrade(gradeOid, auth.getName());
            result.put("success", true);
            result.put("message", "직급이 삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
