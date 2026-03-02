package com.idstory.dept.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.dept.dto.DeptCreateDto;
import com.idstory.dept.dto.DeptUpdateDto;
import com.idstory.dept.entity.Department;
import com.idstory.dept.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 부서 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    // ──────────────────────────────────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 사용중(use_yn='Y')이고 삭제되지 않은 부서 목록을 정렬 순서로 반환합니다.
     * (기존 컨트롤러 호환용)
     */
    public List<Department> findEnabledDepartments() {
        return departmentRepository.findByUseYnAndDeletedAtIsNullOrderBySortOrderAsc("Y");
    }

    /**
     * 조직도용 부서 전체 목록 반환.
     *
     * @param includeDeleted true=소프트 삭제 부서 포함
     */
    public List<Department> findAllForChart(boolean includeDeleted) {
        if (includeDeleted) {
            return departmentRepository.findAllByOrderBySortOrderAsc();
        }
        return departmentRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
    }

    /**
     * 부서코드로 부서를 조회합니다 (없으면 empty).
     */
    public Optional<Department> findByDeptCode(String deptCode) {
        return departmentRepository.findByDeptCode(deptCode);
    }

    /**
     * OID로 부서를 조회합니다. 없으면 예외를 던집니다.
     */
    public Department getDeptByOid(String deptOid) {
        return departmentRepository.findById(deptOid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부서입니다. OID=" + deptOid));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 등록
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 신규 부서를 등록합니다.
     */
    @Transactional
    public Department createDept(DeptCreateDto dto, String performedBy) {
        if (departmentRepository.existsByDeptCode(dto.getDeptCode())) {
            throw new IllegalArgumentException("이미 사용 중인 부서코드입니다: " + dto.getDeptCode());
        }

        Department dept = Department.builder()
                .deptOid(OidGenerator.generate())
                .deptCode(dto.getDeptCode().toUpperCase())
                .deptName(dto.getDeptName())
                .parentDeptOid(blankToNull(dto.getParentDeptOid()))
                .sortOrder(dto.getSortOrder())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : "Y")
                .deptType(blankToNull(dto.getDeptType()))
                .deptTel(blankToNull(dto.getDeptTel()))
                .deptFax(blankToNull(dto.getDeptFax()))
                .deptAddress(blankToNull(dto.getDeptAddress()))
                .createdBy(performedBy)
                .updatedBy(performedBy)
                .build();

        Department saved = departmentRepository.save(dept);
        log.info("[DepartmentService] 부서 등록 완료 - oid: {}, code: {}", saved.getDeptOid(), saved.getDeptCode());
        return saved;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 수정
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 부서 정보를 수정합니다.
     */
    @Transactional
    public Department updateDept(String deptOid, DeptUpdateDto dto, String performedBy) {
        Department dept = getDeptByOid(deptOid);

        dept.setDeptName(dto.getDeptName());
        dept.setParentDeptOid(blankToNull(dto.getParentDeptOid()));
        dept.setSortOrder(dto.getSortOrder());
        dept.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        dept.setDeptType(blankToNull(dto.getDeptType()));
        dept.setDeptTel(blankToNull(dto.getDeptTel()));
        dept.setDeptFax(blankToNull(dto.getDeptFax()));
        dept.setDeptAddress(blankToNull(dto.getDeptAddress()));
        dept.setUpdatedBy(performedBy);

        log.info("[DepartmentService] 부서 수정 완료 - oid: {}", deptOid);
        return dept;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 삭제 / 복원
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 부서를 소프트 삭제합니다 (deleted_at 설정).
     */
    @Transactional
    public void softDeleteDept(String deptOid, String performedBy) {
        Department dept = getDeptByOid(deptOid);
        if (dept.getDeletedAt() != null) {
            throw new IllegalStateException("이미 삭제된 부서입니다.");
        }
        dept.setDeletedAt(LocalDateTime.now());
        dept.setDeletedBy(performedBy);
        dept.setUpdatedBy(performedBy);
        log.info("[DepartmentService] 부서 소프트 삭제 - oid: {}", deptOid);
    }

    /**
     * 소프트 삭제된 부서를 복원합니다.
     */
    @Transactional
    public void restoreDept(String deptOid, String performedBy) {
        Department dept = getDeptByOid(deptOid);
        if (dept.getDeletedAt() == null) {
            throw new IllegalStateException("삭제되지 않은 부서입니다.");
        }
        dept.setDeletedAt(null);
        dept.setDeletedBy(null);
        dept.setUpdatedBy(performedBy);
        log.info("[DepartmentService] 부서 복원 - oid: {}", deptOid);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────────────────────────

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
