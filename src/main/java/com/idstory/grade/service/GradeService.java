package com.idstory.grade.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.grade.dto.GradeCreateDto;
import com.idstory.grade.dto.GradeUpdateDto;
import com.idstory.grade.entity.Grade;
import com.idstory.grade.repository.GradeRepository;
import com.idstory.orghistory.service.OrgHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 직급 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final OrgHistoryService orgHistoryService;

    private static final String TARGET_TYPE = "GRADE";

    // ─────────────────────────────────────────────────────────────
    //  조회
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Grade> findGrades(String gradeCode, String gradeName,
                                  String useYn, boolean includeDeleted, Pageable pageable) {
        return gradeRepository.findByFilter(
                blankToNull(gradeCode),
                blankToNull(gradeName),
                blankToNull(useYn),
                includeDeleted,
                pageable);
    }

    @Transactional(readOnly = true)
    public Grade getByOid(String gradeOid) {
        return gradeRepository.findById(gradeOid)
                .orElseThrow(() -> new IllegalArgumentException("직급을 찾을 수 없습니다: " + gradeOid));
    }

    // ─────────────────────────────────────────────────────────────
    //  등록
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Grade createGrade(GradeCreateDto dto, String performedBy) {
        String code = dto.getGradeCode().toUpperCase().trim();
        if (gradeRepository.existsByGradeCode(code)) {
            throw new IllegalArgumentException("이미 사용 중인 직급코드입니다: " + code);
        }

        Grade g = Grade.builder()
                .gradeOid(OidGenerator.generate())
                .gradeCode(code)
                .gradeName(dto.getGradeName().trim())
                .gradeDesc(blankToNull(dto.getGradeDesc()))
                .sortOrder(dto.getSortOrder())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : "Y")
                .createdBy(performedBy)
                .build();

        gradeRepository.save(g);
        orgHistoryService.log(TARGET_TYPE, g.getGradeOid(), "CREATE", null, toMap(g), performedBy);
        log.info("[GradeService] 직급 등록 - oid: {}, code: {}", g.getGradeOid(), g.getGradeCode());
        return g;
    }

    // ─────────────────────────────────────────────────────────────
    //  수정
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Grade updateGrade(String gradeOid, GradeUpdateDto dto, String performedBy) {
        Grade g = getByOid(gradeOid);
        Map<String, Object> before = toMap(g);

        g.setGradeName(dto.getGradeName().trim());
        g.setGradeDesc(blankToNull(dto.getGradeDesc()));
        g.setSortOrder(dto.getSortOrder());
        g.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        g.setUpdatedBy(performedBy);

        gradeRepository.save(g);
        orgHistoryService.log(TARGET_TYPE, g.getGradeOid(), "UPDATE", before, toMap(g), performedBy);
        log.info("[GradeService] 직급 수정 - oid: {}", g.getGradeOid());
        return g;
    }

    // ─────────────────────────────────────────────────────────────
    //  삭제 (소프트)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteGrade(String gradeOid, String performedBy) {
        Grade g = getByOid(gradeOid);
        if (g.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 삭제된 직급입니다.");
        }
        Map<String, Object> before = toMap(g);

        g.setDeletedAt(LocalDateTime.now());
        g.setDeletedBy(performedBy);
        gradeRepository.save(g);

        orgHistoryService.log(TARGET_TYPE, g.getGradeOid(), "DELETE", before, null, performedBy);
        log.info("[GradeService] 직급 삭제 - oid: {}", g.getGradeOid());
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    public Map<String, Object> toMap(Grade g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gradeOid",  g.getGradeOid());
        m.put("gradeCode", g.getGradeCode());
        m.put("gradeName", g.getGradeName());
        m.put("gradeDesc", g.getGradeDesc());
        m.put("sortOrder", g.getSortOrder());
        m.put("useYn",     g.getUseYn());
        m.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);
        m.put("createdBy", g.getCreatedBy());
        m.put("updatedAt", g.getUpdatedAt() != null ? g.getUpdatedAt().toString() : null);
        m.put("updatedBy", g.getUpdatedBy());
        m.put("deletedAt", g.getDeletedAt() != null ? g.getDeletedAt().toString() : null);
        m.put("deletedBy", g.getDeletedBy());
        return m;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
