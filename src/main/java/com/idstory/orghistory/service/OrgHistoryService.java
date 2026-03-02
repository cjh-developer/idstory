package com.idstory.orghistory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idstory.common.util.OidGenerator;
import com.idstory.orghistory.entity.OrgHistory;
import com.idstory.orghistory.repository.OrgHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 조직 이력 서비스 (직위·직급 통합)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgHistoryService {

    private final OrgHistoryRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 조직 이력을 기록합니다.
     *
     * @param targetType 대상 구분 (POSITION | GRADE)
     * @param targetOid  대상 OID
     * @param actionType 처리 구분 (CREATE | UPDATE | DELETE)
     * @param before     변경 전 데이터 (null 허용)
     * @param after      변경 후 데이터 (null 허용)
     * @param actionBy   처리자
     */
    @Transactional
    public void log(String targetType, String targetOid, String actionType,
                    Object before, Object after, String actionBy) {
        OrgHistory h = OrgHistory.builder()
                .historyOid(OidGenerator.generate())
                .targetType(targetType)
                .targetOid(targetOid)
                .actionType(actionType)
                .beforeData(toJson(before))
                .afterData(toJson(after))
                .actionAt(LocalDateTime.now())
                .actionBy(actionBy)
                .build();
        repository.save(h);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("[OrgHistoryService] JSON 직렬화 실패: {}", e.getMessage());
            return obj.toString();
        }
    }
}
