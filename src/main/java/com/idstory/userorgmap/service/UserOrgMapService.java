package com.idstory.userorgmap.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.userorgmap.entity.UserOrgMap;
import com.idstory.userorgmap.repository.UserOrgMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자-조직 매핑 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrgMapService {

    private final UserOrgMapRepository userOrgMapRepository;

    @Transactional(readOnly = true)
    public List<UserOrgMap> findByUserOid(String userOid) {
        return userOrgMapRepository.findByUserOidOrderByIsPrimaryDescCreatedAtAsc(userOid);
    }

    @Transactional(readOnly = true)
    public List<UserOrgMap> findConcurrentByDeptOid(String deptOid) {
        return userOrgMapRepository.findByDeptOidAndIsPrimary(deptOid, "N");
    }

    @Transactional
    public UserOrgMap addOrgMap(String userOid, String userId, String userName,
                                String deptOid,      String deptName,
                                String positionOid,  String positionName,
                                String gradeOid,     String gradeName,
                                String compRoleOid,  String compRoleName,
                                String isPrimary,    String createdBy) {

        if ("N".equals(isPrimary) && deptOid != null && !deptOid.isBlank()
                && userOrgMapRepository.existsByUserOidAndDeptOidAndIsPrimary(userOid, deptOid, "N")) {
            throw new IllegalArgumentException("이미 겸직으로 등록된 부서입니다.");
        }

        UserOrgMap map = UserOrgMap.builder()
                .mapOid(OidGenerator.generate())
                .userOid(userOid)
                .userId(userId)
                .userName(userName)
                .deptOid(blankToNull(deptOid))
                .deptName(blankToNull(deptName))
                .positionOid(blankToNull(positionOid))
                .positionName(blankToNull(positionName))
                .gradeOid(blankToNull(gradeOid))
                .gradeName(blankToNull(gradeName))
                .compRoleOid(blankToNull(compRoleOid))
                .compRoleName(blankToNull(compRoleName))
                .isPrimary(isPrimary != null ? isPrimary : "Y")
                .createdBy(createdBy)
                .build();

        userOrgMapRepository.save(map);
        log.info("[UserOrgMapService] 조직 매핑 추가 - userOid: {}, isPrimary: {}", userOid, isPrimary);
        return map;
    }

    @Transactional
    public void deleteByMapOid(String mapOid) {
        userOrgMapRepository.deleteById(mapOid);
        log.info("[UserOrgMapService] 조직 매핑 삭제 - mapOid: {}", mapOid);
    }

    public Map<String, Object> toMap(UserOrgMap m) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mapOid",       m.getMapOid());
        result.put("userOid",      m.getUserOid());
        result.put("userId",       m.getUserId());
        result.put("userName",     m.getUserName());
        result.put("deptOid",      m.getDeptOid());
        result.put("deptName",     m.getDeptName());
        result.put("positionOid",  m.getPositionOid());
        result.put("positionName", m.getPositionName());
        result.put("gradeOid",     m.getGradeOid());
        result.put("gradeName",    m.getGradeName());
        result.put("compRoleOid",  m.getCompRoleOid());
        result.put("compRoleName", m.getCompRoleName());
        result.put("isPrimary",    m.getIsPrimary());
        result.put("createdAt",    m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
        result.put("createdBy",    m.getCreatedBy());
        return result;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
