package com.idstory.role.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.role.dto.RoleCreateDto;
import com.idstory.role.dto.RoleUpdateDto;
import com.idstory.role.entity.Role;
import com.idstory.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 역할 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    // ─────────────────────────────────────────────────────────────
    //  조회
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Role> buildTree() {
        return buildTree(findAll());
    }

    @Transactional(readOnly = true)
    public Role getByOid(String roleOid) {
        return roleRepository.findById(roleOid)
                .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다: " + roleOid));
    }

    // ─────────────────────────────────────────────────────────────
    //  등록
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Role create(RoleCreateDto dto, String performedBy) {
        String code = dto.getRoleCode().toUpperCase().trim();
        if (roleRepository.existsByRoleCode(code)) {
            throw new IllegalArgumentException("이미 사용 중인 역할 코드입니다: " + code);
        }

        Role r = Role.builder()
                .roleOid(OidGenerator.generate())
                .roleCode(code)
                .roleName(dto.getRoleName().trim())
                .parentOid(blankToNull(dto.getParentOid()))
                .description(blankToNull(dto.getDescription()))
                .sortOrder(dto.getSortOrder())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : "Y")
                .createdBy(performedBy)
                .build();

        roleRepository.save(r);
        log.info("[RoleService] 역할 등록 - oid: {}, code: {}", r.getRoleOid(), r.getRoleCode());
        return r;
    }

    // ─────────────────────────────────────────────────────────────
    //  수정
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Role update(String roleOid, RoleUpdateDto dto, String performedBy) {
        Role r = getByOid(roleOid);

        String newParent = blankToNull(dto.getParentOid());
        if (roleOid.equals(newParent)) {
            throw new IllegalArgumentException("자기 자신을 상위 역할로 설정할 수 없습니다.");
        }

        r.setRoleName(dto.getRoleName().trim());
        r.setParentOid(newParent);
        r.setDescription(blankToNull(dto.getDescription()));
        r.setSortOrder(dto.getSortOrder());
        r.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        r.setUpdatedBy(performedBy);

        roleRepository.save(r);
        log.info("[RoleService] 역할 수정 - oid: {}", r.getRoleOid());
        return r;
    }

    // ─────────────────────────────────────────────────────────────
    //  삭제 (소프트)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void softDelete(String roleOid, String performedBy) {
        Role r = getByOid(roleOid);
        if (r.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 삭제된 역할입니다.");
        }
        r.setDeletedAt(LocalDateTime.now());
        r.setDeletedBy(performedBy);
        roleRepository.save(r);
        log.info("[RoleService] 역할 삭제 - oid: {}", r.getRoleOid());
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    private List<Role> buildTree(List<Role> all) {
        Map<String, Role> map = new LinkedHashMap<>();
        all.forEach(r -> map.put(r.getRoleOid(), r));

        List<Role> roots = new ArrayList<>();
        for (Role r : all) {
            r.setChildren(new ArrayList<>());
            if (r.getParentOid() == null || !map.containsKey(r.getParentOid())) {
                roots.add(r);
            } else {
                map.get(r.getParentOid()).getChildren().add(r);
            }
        }
        return roots;
    }

    public Map<String, Object> toMap(Role r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roleOid",     r.getRoleOid());
        m.put("roleCode",    r.getRoleCode());
        m.put("roleName",    r.getRoleName());
        m.put("parentOid",   r.getParentOid());
        m.put("description", r.getDescription());
        m.put("sortOrder",   r.getSortOrder());
        m.put("useYn",       r.getUseYn());
        m.put("createdAt",   r.getCreatedAt()  != null ? r.getCreatedAt().toString()  : null);
        m.put("createdBy",   r.getCreatedBy());
        m.put("updatedAt",   r.getUpdatedAt()  != null ? r.getUpdatedAt().toString()  : null);
        m.put("updatedBy",   r.getUpdatedBy());
        m.put("deletedAt",   r.getDeletedAt()  != null ? r.getDeletedAt().toString()  : null);
        m.put("deletedBy",   r.getDeletedBy());
        return m;
    }

    public List<Map<String, Object>> toTreeMapList(List<Role> nodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role r : nodes) {
            Map<String, Object> m = toMap(r);
            if (r.getChildren() != null && !r.getChildren().isEmpty()) {
                m.put("children", toTreeMapList(r.getChildren()));
            } else {
                m.put("children", new ArrayList<>());
            }
            result.add(m);
        }
        return result;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
