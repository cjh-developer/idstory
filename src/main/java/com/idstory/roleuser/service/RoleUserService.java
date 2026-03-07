package com.idstory.roleuser.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.role.repository.RoleRepository;
import com.idstory.roleuser.entity.RoleUser;
import com.idstory.roleuser.repository.RoleUserRepository;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 역할-사용자 매핑 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleUserService {

    private final RoleUserRepository roleUserRepository;
    private final SysUserRepository  sysUserRepository;
    private final RoleRepository     roleRepository;

    /**
     * 역할에 배정된 사용자 목록 (상세 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUsersByRole(String roleOid) {
        List<RoleUser> mappings = roleUserRepository.findByRoleOid(roleOid);
        List<Map<String, Object>> result = new ArrayList<>();

        for (RoleUser ru : mappings) {
            sysUserRepository.findById(ru.getUserOid()).ifPresent(u -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("roleUserOid", ru.getRoleUserOid());
                map.put("roleOid",     ru.getRoleOid());
                map.put("userOid",     ru.getUserOid());
                map.put("userId",      u.getUserId());
                map.put("name",        u.getName());
                map.put("deptCode",    u.getDeptCode());
                map.put("useYn",       u.getUseYn());
                map.put("status",      u.getStatus());
                map.put("createdAt",   ru.getCreatedAt() != null ? ru.getCreatedAt().toString() : null);
                map.put("createdBy",   ru.getCreatedBy());
                result.add(map);
            });
        }
        return result;
    }

    /**
     * 역할에 사용자 배정
     */
    @Transactional
    public RoleUser assign(String roleOid, String userOid, String performedBy) {
        roleRepository.findById(roleOid)
                .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다."));

        sysUserRepository.findById(userOid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (roleUserRepository.existsByRoleOidAndUserOid(roleOid, userOid)) {
            throw new IllegalArgumentException("이미 배정된 사용자입니다.");
        }

        RoleUser ru = RoleUser.builder()
                .roleUserOid(OidGenerator.generate())
                .roleOid(roleOid)
                .userOid(userOid)
                .createdBy(performedBy)
                .build();
        roleUserRepository.save(ru);
        log.info("[RoleUserService] 역할 사용자 배정 - roleOid: {}, userOid: {}", roleOid, userOid);
        return ru;
    }

    /**
     * 역할에서 사용자 해제
     */
    @Transactional
    public void revoke(String roleUserOid) {
        RoleUser ru = roleUserRepository.findById(roleUserOid)
                .orElseThrow(() -> new IllegalArgumentException("배정 정보를 찾을 수 없습니다."));
        roleUserRepository.delete(ru);
        log.info("[RoleUserService] 역할 사용자 해제 - roleUserOid: {}", roleUserOid);
    }

    /**
     * SysUser → 표시용 Map
     */
    private Map<String, Object> userToMap(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("oid",      u.getOid());
        m.put("userId",   u.getUserId());
        m.put("name",     u.getName());
        m.put("deptCode", u.getDeptCode());
        return m;
    }
}
