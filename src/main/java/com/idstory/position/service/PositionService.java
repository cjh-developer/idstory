package com.idstory.position.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.orghistory.service.OrgHistoryService;
import com.idstory.position.dto.PositionCreateDto;
import com.idstory.position.dto.PositionUpdateDto;
import com.idstory.position.entity.Position;
import com.idstory.position.repository.PositionRepository;
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
 * 직위 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final OrgHistoryService  orgHistoryService;

    private static final String TARGET_TYPE = "POSITION";

    // ─────────────────────────────────────────────────────────────
    //  조회
    // ─────────────────────────────────────────────────────────────

    /**
     * 다중 조건 필터 + 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<Position> findPositions(String positionCode, String positionName,
                                        String useYn, boolean includeDeleted, Pageable pageable) {
        return positionRepository.findByFilter(
                blankToNull(positionCode),
                blankToNull(positionName),
                blankToNull(useYn),
                includeDeleted,
                pageable);
    }

    /**
     * OID로 단건 조회
     */
    @Transactional(readOnly = true)
    public Position getByOid(String positionOid) {
        return positionRepository.findById(positionOid)
                .orElseThrow(() -> new IllegalArgumentException("직위를 찾을 수 없습니다: " + positionOid));
    }

    // ─────────────────────────────────────────────────────────────
    //  등록
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Position createPosition(PositionCreateDto dto, String performedBy) {
        String code = dto.getPositionCode().toUpperCase().trim();
        if (positionRepository.existsByPositionCode(code)) {
            throw new IllegalArgumentException("이미 사용 중인 직위코드입니다: " + code);
        }

        Position p = Position.builder()
                .positionOid(OidGenerator.generate())
                .positionCode(code)
                .positionName(dto.getPositionName().trim())
                .positionDesc(blankToNull(dto.getPositionDesc()))
                .sortOrder(dto.getSortOrder())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : "Y")
                .createdBy(performedBy)
                .build();

        positionRepository.save(p);
        orgHistoryService.log(TARGET_TYPE, p.getPositionOid(), "CREATE", null, toMap(p), performedBy);
        log.info("[PositionService] 직위 등록 - oid: {}, code: {}", p.getPositionOid(), p.getPositionCode());
        return p;
    }

    // ─────────────────────────────────────────────────────────────
    //  수정
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Position updatePosition(String positionOid, PositionUpdateDto dto, String performedBy) {
        Position p = getByOid(positionOid);
        Map<String, Object> before = toMap(p);

        p.setPositionName(dto.getPositionName().trim());
        p.setPositionDesc(blankToNull(dto.getPositionDesc()));
        p.setSortOrder(dto.getSortOrder());
        p.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        p.setUpdatedBy(performedBy);

        positionRepository.save(p);
        orgHistoryService.log(TARGET_TYPE, p.getPositionOid(), "UPDATE", before, toMap(p), performedBy);
        log.info("[PositionService] 직위 수정 - oid: {}", p.getPositionOid());
        return p;
    }

    // ─────────────────────────────────────────────────────────────
    //  삭제 (소프트)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void deletePosition(String positionOid, String performedBy) {
        Position p = getByOid(positionOid);
        if (p.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 삭제된 직위입니다.");
        }
        Map<String, Object> before = toMap(p);

        p.setDeletedAt(LocalDateTime.now());
        p.setDeletedBy(performedBy);
        positionRepository.save(p);

        orgHistoryService.log(TARGET_TYPE, p.getPositionOid(), "DELETE", before, null, performedBy);
        log.info("[PositionService] 직위 삭제 - oid: {}", p.getPositionOid());
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    public Map<String, Object> toMap(Position p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("positionOid",  p.getPositionOid());
        m.put("positionCode", p.getPositionCode());
        m.put("positionName", p.getPositionName());
        m.put("positionDesc", p.getPositionDesc());
        m.put("sortOrder",    p.getSortOrder());
        m.put("useYn",        p.getUseYn());
        m.put("createdAt",    p.getCreatedAt()  != null ? p.getCreatedAt().toString()  : null);
        m.put("createdBy",    p.getCreatedBy());
        m.put("updatedAt",    p.getUpdatedAt()  != null ? p.getUpdatedAt().toString()  : null);
        m.put("updatedBy",    p.getUpdatedBy());
        m.put("deletedAt",    p.getDeletedAt()  != null ? p.getDeletedAt().toString()  : null);
        m.put("deletedBy",    p.getDeletedBy());
        return m;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
