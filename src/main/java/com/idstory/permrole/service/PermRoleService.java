package com.idstory.permrole.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.permrole.entity.PermRole;
import com.idstory.permrole.repository.PermRoleRepository;
import com.idstory.role.entity.Role;
import com.idstory.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 권한-역할 매핑 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermRoleService {

    private final PermRoleRepository permRoleRepository;
    private final RoleRepository     roleRepository;

    /**
     * 특정 권한에 배정된/미배정 역할 목록 반환
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRolesForPerm(String permOid) {
        // 배정된 역할 OID set
        Set<String> assignedOids = new HashSet<>();
        List<PermRole> mappings = permRoleRepository.findByPermOid(permOid);
        mappings.forEach(m -> assignedOids.add(m.getRoleOid()));

        // 전체 활성 역할
        List<Role> allRoles = roleRepository.findByDeletedAtIsNullOrderBySortOrderAsc();

        List<Map<String, Object>> assigned   = new ArrayList<>();
        List<Map<String, Object>> unassigned = new ArrayList<>();

        for (Role r : allRoles) {
            Map<String, Object> rm = roleToMap(r);
            if (assignedOids.contains(r.getRoleOid())) {
                assigned.add(rm);
            } else {
                unassigned.add(rm);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assigned",   assigned);
        result.put("unassigned", unassigned);
        return result;
    }

    /**
     * 역할 배정
     */
    @Transactional
    public void assign(String permOid, String roleOid, String performedBy) {
        if (permRoleRepository.findByPermOidAndRoleOid(permOid, roleOid).isPresent()) {
            throw new IllegalArgumentException("이미 배정된 역할입니다.");
        }

        PermRole pr = PermRole.builder()
                .permRoleOid(OidGenerator.generate())
                .permOid(permOid)
                .roleOid(roleOid)
                .createdBy(performedBy)
                .build();

        permRoleRepository.save(pr);
        log.info("[PermRoleService] 역할 배정 - permOid: {}, roleOid: {}", permOid, roleOid);
    }

    /**
     * 역할 해제
     */
    @Transactional
    public void revoke(String permOid, String roleOid, String performedBy) {
        permRoleRepository.findByPermOidAndRoleOid(permOid, roleOid)
                .orElseThrow(() -> new IllegalArgumentException("배정되지 않은 역할입니다."));
        permRoleRepository.deleteByPermOidAndRoleOid(permOid, roleOid);
        log.info("[PermRoleService] 역할 해제 - permOid: {}, roleOid: {}", permOid, roleOid);
    }

    private Map<String, Object> roleToMap(Role r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roleOid",     r.getRoleOid());
        m.put("roleCode",    r.getRoleCode());
        m.put("roleName",    r.getRoleName());
        m.put("description", r.getDescription());
        m.put("useYn",       r.getUseYn());
        return m;
    }
}
