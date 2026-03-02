package com.idstory.comprole.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.comprole.dto.CompRoleCreateDto;
import com.idstory.comprole.dto.CompRoleUpdateDto;
import com.idstory.comprole.entity.CompRole;
import com.idstory.comprole.repository.CompRoleRepository;
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
 * 직책 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompRoleService {

    private final CompRoleRepository compRoleRepository;
    private final OrgHistoryService orgHistoryService;

    private static final String TARGET_TYPE = "COMP_ROLE";

    // ─────────────────────────────────────────────────────────────
    //  조회
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CompRole> findCompRoles(String compRoleCode, String compRoleName,
                                        String useYn, boolean includeDeleted, Pageable pageable) {
        return compRoleRepository.findByFilter(
                blankToNull(compRoleCode),
                blankToNull(compRoleName),
                blankToNull(useYn),
                includeDeleted,
                pageable);
    }

    @Transactional(readOnly = true)
    public CompRole getByOid(String compRoleOid) {
        return compRoleRepository.findById(compRoleOid)
                .orElseThrow(() -> new IllegalArgumentException("직책을 찾을 수 없습니다: " + compRoleOid));
    }

    // ─────────────────────────────────────────────────────────────
    //  등록
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public CompRole createCompRole(CompRoleCreateDto dto, String performedBy) {
        String code = dto.getCompRoleCode().toUpperCase().trim();
        if (compRoleRepository.existsByCompRoleCode(code)) {
            throw new IllegalArgumentException("이미 사용 중인 직책코드입니다: " + code);
        }

        CompRole r = CompRole.builder()
                .compRoleOid(OidGenerator.generate())
                .compRoleCode(code)
                .compRoleName(dto.getCompRoleName().trim())
                .compRoleDesc(blankToNull(dto.getCompRoleDesc()))
                .sortOrder(dto.getSortOrder())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : "Y")
                .createdBy(performedBy)
                .build();

        compRoleRepository.save(r);
        orgHistoryService.log(TARGET_TYPE, r.getCompRoleOid(), "CREATE", null, toMap(r), performedBy);
        log.info("[CompRoleService] 직책 등록 - oid: {}, code: {}", r.getCompRoleOid(), r.getCompRoleCode());
        return r;
    }

    // ─────────────────────────────────────────────────────────────
    //  수정
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public CompRole updateCompRole(String compRoleOid, CompRoleUpdateDto dto, String performedBy) {
        CompRole r = getByOid(compRoleOid);
        Map<String, Object> before = toMap(r);

        r.setCompRoleName(dto.getCompRoleName().trim());
        r.setCompRoleDesc(blankToNull(dto.getCompRoleDesc()));
        r.setSortOrder(dto.getSortOrder());
        r.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        r.setUpdatedBy(performedBy);

        compRoleRepository.save(r);
        orgHistoryService.log(TARGET_TYPE, r.getCompRoleOid(), "UPDATE", before, toMap(r), performedBy);
        log.info("[CompRoleService] 직책 수정 - oid: {}", r.getCompRoleOid());
        return r;
    }

    // ─────────────────────────────────────────────────────────────
    //  삭제 (소프트)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteCompRole(String compRoleOid, String performedBy) {
        CompRole r = getByOid(compRoleOid);
        if (r.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 삭제된 직책입니다.");
        }
        Map<String, Object> before = toMap(r);

        r.setDeletedAt(LocalDateTime.now());
        r.setDeletedBy(performedBy);
        compRoleRepository.save(r);

        orgHistoryService.log(TARGET_TYPE, r.getCompRoleOid(), "DELETE", before, null, performedBy);
        log.info("[CompRoleService] 직책 삭제 - oid: {}", r.getCompRoleOid());
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    public Map<String, Object> toMap(CompRole r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compRoleOid",  r.getCompRoleOid());
        m.put("compRoleCode", r.getCompRoleCode());
        m.put("compRoleName", r.getCompRoleName());
        m.put("compRoleDesc", r.getCompRoleDesc());
        m.put("sortOrder",    r.getSortOrder());
        m.put("useYn",        r.getUseYn());
        m.put("createdAt",    r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        m.put("createdBy",    r.getCreatedBy());
        m.put("updatedAt",    r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);
        m.put("updatedBy",    r.getUpdatedBy());
        m.put("deletedAt",    r.getDeletedAt() != null ? r.getDeletedAt().toString() : null);
        m.put("deletedBy",    r.getDeletedBy());
        return m;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
