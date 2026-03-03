package com.idstory.permission.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.permission.dto.PermissionCreateDto;
import com.idstory.permission.dto.PermissionUpdateDto;
import com.idstory.permission.entity.Permission;
import com.idstory.permission.repository.PermissionRepository;
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
 * 권한 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    // ─────────────────────────────────────────────────────────────
    //  조회
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Permission> buildTree(String clientOid) {
        List<Permission> all = permissionRepository.findByClientOidAndDeletedAtIsNullOrderBySortOrderAsc(clientOid);
        return buildTree(all);
    }

    @Transactional(readOnly = true)
    public Permission getByOid(String permOid) {
        return permissionRepository.findById(permOid)
                .orElseThrow(() -> new IllegalArgumentException("권한을 찾을 수 없습니다: " + permOid));
    }

    // ─────────────────────────────────────────────────────────────
    //  등록
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Permission create(PermissionCreateDto dto, String performedBy) {
        String code = dto.getPermCode().toUpperCase().trim();
        if (permissionRepository.existsByPermCode(code)) {
            throw new IllegalArgumentException("이미 사용 중인 권한 코드입니다: " + code);
        }

        Permission p = Permission.builder()
                .permOid(OidGenerator.generate())
                .clientOid(dto.getClientOid())
                .permCode(code)
                .permName(dto.getPermName().trim())
                .parentOid(blankToNull(dto.getParentOid()))
                .description(blankToNull(dto.getDescription()))
                .sortOrder(dto.getSortOrder())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : "Y")
                .createdBy(performedBy)
                .build();

        permissionRepository.save(p);
        log.info("[PermissionService] 권한 등록 - oid: {}, code: {}", p.getPermOid(), p.getPermCode());
        return p;
    }

    // ─────────────────────────────────────────────────────────────
    //  수정
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Permission update(String permOid, PermissionUpdateDto dto, String performedBy) {
        Permission p = getByOid(permOid);

        String newParent = blankToNull(dto.getParentOid());
        if (permOid.equals(newParent)) {
            throw new IllegalArgumentException("자기 자신을 상위 권한으로 설정할 수 없습니다.");
        }

        p.setPermName(dto.getPermName().trim());
        p.setParentOid(newParent);
        p.setDescription(blankToNull(dto.getDescription()));
        p.setSortOrder(dto.getSortOrder());
        p.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        p.setUpdatedBy(performedBy);

        permissionRepository.save(p);
        log.info("[PermissionService] 권한 수정 - oid: {}", p.getPermOid());
        return p;
    }

    // ─────────────────────────────────────────────────────────────
    //  삭제 (소프트)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void softDelete(String permOid, String performedBy) {
        Permission p = getByOid(permOid);
        if (p.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 삭제된 권한입니다.");
        }
        p.setDeletedAt(LocalDateTime.now());
        p.setDeletedBy(performedBy);
        permissionRepository.save(p);
        log.info("[PermissionService] 권한 삭제 - oid: {}", p.getPermOid());
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    private List<Permission> buildTree(List<Permission> all) {
        Map<String, Permission> map = new LinkedHashMap<>();
        all.forEach(p -> map.put(p.getPermOid(), p));

        List<Permission> roots = new ArrayList<>();
        for (Permission p : all) {
            p.setChildren(new ArrayList<>());
            if (p.getParentOid() == null || !map.containsKey(p.getParentOid())) {
                roots.add(p);
            } else {
                map.get(p.getParentOid()).getChildren().add(p);
            }
        }
        return roots;
    }

    public Map<String, Object> toMap(Permission p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("permOid",     p.getPermOid());
        m.put("clientOid",   p.getClientOid());
        m.put("permCode",    p.getPermCode());
        m.put("permName",    p.getPermName());
        m.put("parentOid",   p.getParentOid());
        m.put("description", p.getDescription());
        m.put("sortOrder",   p.getSortOrder());
        m.put("useYn",       p.getUseYn());
        m.put("createdAt",   p.getCreatedAt()  != null ? p.getCreatedAt().toString()  : null);
        m.put("createdBy",   p.getCreatedBy());
        m.put("updatedAt",   p.getUpdatedAt()  != null ? p.getUpdatedAt().toString()  : null);
        m.put("updatedBy",   p.getUpdatedBy());
        m.put("deletedAt",   p.getDeletedAt()  != null ? p.getDeletedAt().toString()  : null);
        m.put("deletedBy",   p.getDeletedBy());
        return m;
    }

    public List<Map<String, Object>> toTreeMapList(List<Permission> nodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Permission p : nodes) {
            Map<String, Object> m = toMap(p);
            if (p.getChildren() != null && !p.getChildren().isEmpty()) {
                m.put("children", toTreeMapList(p.getChildren()));
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
