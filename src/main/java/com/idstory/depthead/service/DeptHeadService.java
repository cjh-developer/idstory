package com.idstory.depthead.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.depthead.entity.DeptHead;
import com.idstory.depthead.repository.DeptHeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 부서장 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptHeadService {

    private final DeptHeadRepository deptHeadRepository;

    @Transactional(readOnly = true)
    public Optional<DeptHead> findByDeptOid(String deptOid) {
        return deptHeadRepository.findByDeptOid(deptOid);
    }

    /**
     * 부서장 등록/교체 — 기존 부서장이 있으면 삭제 후 신규 저장
     */
    @Transactional
    public DeptHead assignHead(String deptOid, String deptName,
                               String userOid, String userId, String userName,
                               String performedBy) {
        // 기존 부서장 삭제 (교체 허용)
        deptHeadRepository.findByDeptOid(deptOid).ifPresent(existing -> {
            deptHeadRepository.deleteById(existing.getHeadOid());
            deptHeadRepository.flush();
        });

        DeptHead head = DeptHead.builder()
                .headOid(OidGenerator.generate())
                .deptOid(deptOid)
                .deptName(deptName)
                .userOid(userOid)
                .userId(userId)
                .userName(userName)
                .createdBy(performedBy)
                .updatedBy(performedBy)
                .build();

        deptHeadRepository.save(head);
        log.info("[DeptHeadService] 부서장 등록/교체 - deptOid: {}, userOid: {}", deptOid, userOid);
        return head;
    }

    /**
     * 부서장 해제
     */
    @Transactional
    public void removeHead(String headOid) {
        deptHeadRepository.deleteById(headOid);
        log.info("[DeptHeadService] 부서장 해제 - headOid: {}", headOid);
    }
}
