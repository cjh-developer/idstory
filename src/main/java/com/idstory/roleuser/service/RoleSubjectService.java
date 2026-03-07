package com.idstory.roleuser.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.dept.entity.Department;
import com.idstory.dept.repository.DepartmentRepository;
import com.idstory.grade.entity.Grade;
import com.idstory.grade.repository.GradeRepository;
import com.idstory.position.entity.Position;
import com.idstory.position.repository.PositionRepository;
import com.idstory.role.repository.RoleRepository;
import com.idstory.roleuser.entity.RoleSubject;
import com.idstory.roleuser.entity.RoleUser;
import com.idstory.roleuser.repository.RoleSubjectRepository;
import com.idstory.roleuser.repository.RoleUserRepository;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import com.idstory.userorgmap.entity.UserOrgMap;
import com.idstory.userorgmap.repository.UserOrgMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 역할 대상(부서/직위/직급/예외) 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleSubjectService {

    private final RoleSubjectRepository roleSubjectRepository;
    private final RoleUserRepository    roleUserRepository;
    private final RoleRepository        roleRepository;
    private final DepartmentRepository  departmentRepository;
    private final SysUserRepository     sysUserRepository;
    private final GradeRepository       gradeRepository;
    private final PositionRepository    positionRepository;
    private final UserOrgMapRepository  userOrgMapRepository;

    // ─────────────────────────────────────────────────────────────
    //  조회
    // ─────────────────────────────────────────────────────────────

    /**
     * 역할의 특정 유형 대상 목록 (상세 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSubjectsWithDetails(String roleOid, String subjectType) {
        List<RoleSubject> subjects = roleSubjectRepository.findByRoleOidAndSubjectType(roleOid, subjectType);
        List<Map<String, Object>> result = new ArrayList<>();

        for (RoleSubject s : subjects) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("roleSubjectOid",  s.getRoleSubjectOid());
            map.put("roleOid",         s.getRoleOid());
            map.put("subjectType",     s.getSubjectType());
            map.put("subjectOid",      s.getSubjectOid());
            map.put("includeChildren", s.getIncludeChildren());
            map.put("createdAt",       s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            map.put("createdBy",       s.getCreatedBy());

            switch (subjectType) {
                case "DEPT" -> departmentRepository.findById(s.getSubjectOid()).ifPresent(d -> {
                    map.put("displayName", d.getDeptName());
                    map.put("displayCode", d.getDeptCode());
                });
                case "POSITION" -> positionRepository.findById(s.getSubjectOid()).ifPresent(p -> {
                    map.put("displayName", p.getPositionName());
                    map.put("displayCode", p.getPositionCode());
                });
                case "GRADE" -> gradeRepository.findById(s.getSubjectOid()).ifPresent(g -> {
                    map.put("displayName", g.getGradeName());
                    map.put("displayCode", g.getGradeCode());
                });
                case "EXCEPTION" -> sysUserRepository.findById(s.getSubjectOid()).ifPresent(u -> {
                    map.put("displayName", u.getName());
                    map.put("displayCode", u.getUserId());
                });
            }
            result.add(map);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    //  배정
    // ─────────────────────────────────────────────────────────────

    /**
     * 부서 다중 배정 (includeChildren 공통 적용)
     */
    @Transactional
    public List<String> assignDepts(String roleOid, List<String> deptOids,
                                    String includeChildren, String performedBy) {
        roleRepository.findById(roleOid)
                .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다."));

        List<String> assigned = new ArrayList<>();
        List<String> skipped  = new ArrayList<>();

        for (String deptOid : deptOids) {
            Department dept = departmentRepository.findById(deptOid).orElse(null);
            if (dept == null) { skipped.add(deptOid); continue; }

            if (roleSubjectRepository.existsByRoleOidAndSubjectTypeAndSubjectOid(roleOid, "DEPT", deptOid)) {
                skipped.add(dept.getDeptName());
                continue;
            }
            RoleSubject rs = RoleSubject.builder()
                    .roleSubjectOid(OidGenerator.generate())
                    .roleOid(roleOid)
                    .subjectType("DEPT")
                    .subjectOid(deptOid)
                    .includeChildren("Y".equals(includeChildren) ? "Y" : "N")
                    .createdBy(performedBy)
                    .build();
            roleSubjectRepository.save(rs);
            assigned.add(dept.getDeptName());
        }
        log.info("[RoleSubjectService] 부서 배정 완료 roleOid={}, 성공={}, 중복={}",
                roleOid, assigned.size(), skipped.size());
        return assigned;
    }

    /**
     * 단건 배정 (POSITION / GRADE / EXCEPTION)
     */
    @Transactional
    public RoleSubject assign(String roleOid, String subjectType, String subjectOid, String performedBy) {
        roleRepository.findById(roleOid)
                .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다."));

        if (roleSubjectRepository.existsByRoleOidAndSubjectTypeAndSubjectOid(roleOid, subjectType, subjectOid)) {
            throw new IllegalArgumentException("이미 배정된 항목입니다.");
        }

        RoleSubject rs = RoleSubject.builder()
                .roleSubjectOid(OidGenerator.generate())
                .roleOid(roleOid)
                .subjectType(subjectType)
                .subjectOid(subjectOid)
                .includeChildren("N")
                .createdBy(performedBy)
                .build();
        roleSubjectRepository.save(rs);
        log.info("[RoleSubjectService] 배정 roleOid={}, type={}, subjectOid={}", roleOid, subjectType, subjectOid);
        return rs;
    }

    /**
     * 배정 해제
     */
    @Transactional
    public void revoke(String roleSubjectOid) {
        RoleSubject rs = roleSubjectRepository.findById(roleSubjectOid)
                .orElseThrow(() -> new IllegalArgumentException("배정 정보를 찾을 수 없습니다."));
        roleSubjectRepository.delete(rs);
        log.info("[RoleSubjectService] 배정 해제 roleSubjectOid={}", roleSubjectOid);
    }

    // ─────────────────────────────────────────────────────────────
    //  유효 사용자 계산
    // ─────────────────────────────────────────────────────────────

    /**
     * 역할의 유효 사용자 계산
     * 포함: 개인 배정 + 부서(+하위) 소속 + 직위/직급 해당 사용자
     * 제외: 예외 사용자
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEffectiveUsers(String roleOid) {
        // 부서 전체 로드 (트리 탐색용)
        List<Department> allDepts = departmentRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
        Map<String, List<String>> childMap = buildChildMap(allDepts); // parentOid → childOids
        Map<String, String>       deptCodeMap = new HashMap<>();      // deptOid → deptCode
        allDepts.forEach(d -> deptCodeMap.put(d.getDeptOid(), d.getDeptCode()));

        Set<String> includeOids = new LinkedHashSet<>();
        Set<String> excludeOids = new HashSet<>();

        // 1. 개인 직접 배정 (ids_iam_role_user)
        roleUserRepository.findByRoleOid(roleOid)
                .forEach(ru -> includeOids.add(ru.getUserOid()));

        // 2. 대상 배정 (ids_iam_role_subject)
        List<RoleSubject> subjects = roleSubjectRepository.findByRoleOid(roleOid);
        for (RoleSubject s : subjects) {
            switch (s.getSubjectType()) {
                case "DEPT" -> {
                    Set<String> deptOids = new LinkedHashSet<>();
                    deptOids.add(s.getSubjectOid());
                    if ("Y".equals(s.getIncludeChildren())) {
                        collectDescendants(s.getSubjectOid(), childMap, deptOids);
                    }
                    for (String deptOid : deptOids) {
                        String deptCode = deptCodeMap.get(deptOid);
                        if (deptCode != null) {
                            sysUserRepository.findByDeptCode(deptCode, null,
                                    org.springframework.data.domain.Pageable.unpaged())
                                    .getContent()
                                    .forEach(u -> includeOids.add(u.getOid()));
                        }
                    }
                }
                case "GRADE" -> userOrgMapRepository.findByGradeOid(s.getSubjectOid())
                        .forEach(m -> includeOids.add(m.getUserOid()));
                case "POSITION" -> userOrgMapRepository.findByPositionOid(s.getSubjectOid())
                        .forEach(m -> includeOids.add(m.getUserOid()));
                case "EXCEPTION" -> excludeOids.add(s.getSubjectOid());
            }
        }

        // 3. 예외 사용자 제거
        includeOids.removeAll(excludeOids);

        // 4. 사용자 상세 조회
        List<SysUser> users = sysUserRepository.findAllById(includeOids);
        List<Map<String, Object>> userMaps = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("oid",      u.getOid());
            m.put("userId",   u.getUserId());
            m.put("name",     u.getName());
            m.put("deptCode", u.getDeptCode());
            m.put("useYn",    u.getUseYn());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users",         userMaps);
        result.put("count",         userMaps.size());
        result.put("excludedCount", excludeOids.size());
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    //  사용자 역할 자격 조회
    // ─────────────────────────────────────────────────────────────

    /**
     * 사용자가 DEPT/GRADE/POSITION 배정을 통해 받는 역할 OID 목록
     * EXCEPTION 에 등록된 경우 해당 역할은 제외
     */
    @Transactional(readOnly = true)
    public Set<String> getUserQualifyingRoleOids(String userOid) {
        SysUser user = sysUserRepository.findById(userOid).orElse(null);
        if (user == null) return Collections.emptySet();

        // 부서 트리 (include_children 처리)
        List<Department> allDepts = departmentRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
        Map<String, List<String>> childMap    = buildChildMap(allDepts);
        Map<String, String>       deptOidToCode = new HashMap<>();
        allDepts.forEach(d -> deptOidToCode.put(d.getDeptOid(), d.getDeptCode()));

        // 사용자 조직 정보
        String userDeptCode = user.getDeptCode();
        List<UserOrgMap> orgMaps = userOrgMapRepository.findByUserOidOrderByIsPrimaryDescCreatedAtAsc(userOid);
        Set<String> userGradeOids    = orgMaps.stream().map(UserOrgMap::getGradeOid)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> userPositionOids = orgMaps.stream().map(UserOrgMap::getPositionOid)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        // roleOid → subjects 그룹
        Map<String, List<RoleSubject>> byRole = roleSubjectRepository.findAll().stream()
                .collect(Collectors.groupingBy(RoleSubject::getRoleOid));

        Set<String> result = new LinkedHashSet<>();

        for (Map.Entry<String, List<RoleSubject>> entry : byRole.entrySet()) {
            String roleOid      = entry.getKey();
            boolean qualifies   = false;
            boolean isException = false;

            for (RoleSubject s : entry.getValue()) {
                switch (s.getSubjectType()) {
                    case "DEPT" -> {
                        Department dept = departmentRepository.findById(s.getSubjectOid()).orElse(null);
                        if (dept == null) continue;
                        Set<String> applicableOids = new LinkedHashSet<>();
                        applicableOids.add(s.getSubjectOid());
                        if ("Y".equals(s.getIncludeChildren())) {
                            collectDescendants(s.getSubjectOid(), childMap, applicableOids);
                        }
                        Set<String> applicableCodes = applicableOids.stream()
                                .map(deptOidToCode::get).filter(Objects::nonNull).collect(Collectors.toSet());
                        if (userDeptCode != null && applicableCodes.contains(userDeptCode)) qualifies = true;
                    }
                    case "GRADE"    -> { if (userGradeOids.contains(s.getSubjectOid()))    qualifies = true; }
                    case "POSITION" -> { if (userPositionOids.contains(s.getSubjectOid())) qualifies = true; }
                    case "EXCEPTION" -> { if (userOid.equals(s.getSubjectOid())) isException = true; }
                }
            }

            if (qualifies && !isException) result.add(roleOid);
        }
        return result;
    }

    /**
     * 특정 역할에서 해당 사용자를 포함하는 부서 배정 이름 목록
     * (개인 배정 시 경고 표시용)
     */
    @Transactional(readOnly = true)
    public List<String> getUserCoveringDeptNames(String roleOid, String userOid) {
        SysUser user = sysUserRepository.findById(userOid).orElse(null);
        if (user == null || user.getDeptCode() == null) return Collections.emptyList();

        List<Department> allDepts = departmentRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
        Map<String, List<String>> childMap    = buildChildMap(allDepts);
        Map<String, String>       deptOidToCode = new HashMap<>();
        allDepts.forEach(d -> deptOidToCode.put(d.getDeptOid(), d.getDeptCode()));

        List<String> coveringNames = new ArrayList<>();
        for (RoleSubject s : roleSubjectRepository.findByRoleOidAndSubjectType(roleOid, "DEPT")) {
            Department dept = departmentRepository.findById(s.getSubjectOid()).orElse(null);
            if (dept == null) continue;
            Set<String> applicableOids = new LinkedHashSet<>();
            applicableOids.add(s.getSubjectOid());
            if ("Y".equals(s.getIncludeChildren())) {
                collectDescendants(s.getSubjectOid(), childMap, applicableOids);
            }
            Set<String> applicableCodes = applicableOids.stream()
                    .map(deptOidToCode::get).filter(Objects::nonNull).collect(Collectors.toSet());
            if (applicableCodes.contains(user.getDeptCode())) {
                coveringNames.add(dept.getDeptName() + ("Y".equals(s.getIncludeChildren()) ? " (하위 포함)" : ""));
            }
        }
        return coveringNames;
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    /** parentOid → childOid[] 맵 구성 */
    private Map<String, List<String>> buildChildMap(List<Department> depts) {
        Map<String, List<String>> map = new HashMap<>();
        for (Department d : depts) {
            if (d.getParentDeptOid() != null) {
                map.computeIfAbsent(d.getParentDeptOid(), k -> new ArrayList<>()).add(d.getDeptOid());
            }
        }
        return map;
    }

    /** 하위 부서 OID를 재귀적으로 수집 */
    private void collectDescendants(String parentOid, Map<String, List<String>> childMap, Set<String> collected) {
        List<String> children = childMap.getOrDefault(parentOid, Collections.emptyList());
        for (String childOid : children) {
            if (collected.add(childOid)) {
                collectDescendants(childOid, childMap, collected);
            }
        }
    }
}
