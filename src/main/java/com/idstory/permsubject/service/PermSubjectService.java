package com.idstory.permsubject.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.dept.entity.Department;
import com.idstory.dept.repository.DepartmentRepository;
import com.idstory.grade.entity.Grade;
import com.idstory.grade.repository.GradeRepository;
import com.idstory.permsubject.entity.PermSubject;
import com.idstory.permsubject.repository.PermSubjectRepository;
import com.idstory.position.entity.Position;
import com.idstory.position.repository.PositionRepository;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 권한 대상(부서/개인/직급/직위/예외) 서비스
 */
@Service
@RequiredArgsConstructor
public class PermSubjectService {

    private final PermSubjectRepository permSubjectRepository;
    private final DepartmentRepository  departmentRepository;
    private final SysUserRepository     sysUserRepository;
    private final GradeRepository       gradeRepository;
    private final PositionRepository    positionRepository;

    /**
     * 권한의 특정 유형 대상 목록 (상세 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSubjectsWithDetails(String permOid, String subjectType) {
        List<PermSubject> subjects = permSubjectRepository.findByPermOidAndSubjectType(permOid, subjectType);
        List<Map<String, Object>> result = new ArrayList<>();

        for (PermSubject s : subjects) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("permSubjectOid", s.getPermSubjectOid());
            map.put("permOid",        s.getPermOid());
            map.put("subjectType",    s.getSubjectType());
            map.put("subjectOid",     s.getSubjectOid());
            map.put("createdAt",      s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            map.put("createdBy",      s.getCreatedBy());

            switch (subjectType) {
                case "DEPT" -> departmentRepository.findById(s.getSubjectOid()).ifPresent(d -> {
                    map.put("displayName", d.getDeptName());
                    map.put("displayCode", d.getDeptCode());
                });
                case "USER", "EXCEPTION" -> sysUserRepository.findById(s.getSubjectOid()).ifPresent(u -> {
                    map.put("displayName", u.getName());
                    map.put("displayCode", u.getUserId());
                });
                case "GRADE" -> gradeRepository.findById(s.getSubjectOid()).ifPresent(g -> {
                    map.put("displayName", g.getGradeName());
                    map.put("displayCode", g.getGradeCode());
                });
                case "POSITION" -> positionRepository.findById(s.getSubjectOid()).ifPresent(p -> {
                    map.put("displayName", p.getPositionName());
                    map.put("displayCode", p.getPositionCode());
                });
            }

            result.add(map);
        }
        return result;
    }

    /**
     * 대상 배정
     */
    @Transactional
    public PermSubject assign(String permOid, String subjectType, String subjectOid, String performedBy) {
        permSubjectRepository.findByPermOidAndSubjectTypeAndSubjectOid(permOid, subjectType, subjectOid)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("이미 배정된 대상입니다.");
                });

        PermSubject ps = PermSubject.builder()
                .permSubjectOid(OidGenerator.generate())
                .permOid(permOid)
                .subjectType(subjectType)
                .subjectOid(subjectOid)
                .createdBy(performedBy)
                .build();
        return permSubjectRepository.save(ps);
    }

    /**
     * 대상 배정 해제
     */
    @Transactional
    public void revoke(String permSubjectOid) {
        PermSubject ps = permSubjectRepository.findById(permSubjectOid)
                .orElseThrow(() -> new IllegalArgumentException("배정 정보를 찾을 수 없습니다."));
        permSubjectRepository.delete(ps);
    }

    /**
     * 유효 사용자 계산
     * - DEPT 대상: 해당 부서의 사용자 포함
     * - USER 대상: 직접 포함
     * - EXCEPTION 대상: 제외
     * - GRADE/POSITION: 기록은 있으나 사용자와의 연결 정보 없어 계산 불가
     *   → hasGradeOrPositionRule=true 플래그 반환
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEffectiveUsers(String permOid) {
        List<PermSubject> subjects = permSubjectRepository.findByPermOid(permOid);

        Set<String> includeOids = new LinkedHashSet<>();
        Set<String> excludeOids = new HashSet<>();
        boolean hasGradeOrPositionRule = false;

        for (PermSubject s : subjects) {
            switch (s.getSubjectType()) {
                case "DEPT" -> {
                    departmentRepository.findById(s.getSubjectOid()).ifPresent(dept -> {
                        // dept_code 기준으로 해당 부서 소속 사용자 조회
                        sysUserRepository.findByDeptCode(dept.getDeptCode(), null, Pageable.unpaged())
                                .getContent()
                                .forEach(u -> includeOids.add(u.getOid()));
                    });
                }
                case "USER" -> includeOids.add(s.getSubjectOid());
                case "EXCEPTION" -> excludeOids.add(s.getSubjectOid());
                case "GRADE", "POSITION" -> hasGradeOrPositionRule = true;
            }
        }

        // 예외 사용자 제외
        includeOids.removeAll(excludeOids);

        // 사용자 상세 조회
        List<SysUser> users = sysUserRepository.findAllById(includeOids);
        List<Map<String, Object>> userMaps = new ArrayList<>();
        for (SysUser u : users) {
            Map<String, Object> um = new LinkedHashMap<>();
            um.put("oid",      u.getOid());
            um.put("userId",   u.getUserId());
            um.put("name",     u.getName());
            um.put("deptCode", u.getDeptCode());
            userMaps.add(um);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users",                 userMaps);
        result.put("count",                 userMaps.size());
        result.put("excludedCount",         excludeOids.size());
        result.put("hasGradeOrPositionRule", hasGradeOrPositionRule);
        return result;
    }
}
