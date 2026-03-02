package com.idstory.history.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.history.entity.UserAccountHistory;
import com.idstory.history.repository.UserAccountHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사용자 계정 이력 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountHistoryService {

    private final UserAccountHistoryRepository repository;

    /**
     * 사용자 계정 이력을 기록합니다.
     *
     * @param targetOid      대상 사용자 OID
     * @param targetUsername 대상 사용자 계정
     * @param actionType     처리 유형 (CREATE|UPDATE|DELETE|LOCK|UNLOCK|RESET_PWD)
     * @param detail         변경 상세
     * @param performedBy    처리자 username
     * @param ip             IP 주소
     */
    @Transactional
    public void log(String targetOid, String targetUsername, String actionType,
                    String detail, String performedBy, String ip) {
        UserAccountHistory h = UserAccountHistory.builder()
                .histOid(OidGenerator.generate())
                .targetUserOid(targetOid)
                .targetUsername(targetUsername)
                .actionType(actionType)
                .actionDetail(detail)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .ipAddress(ip)
                .build();
        repository.save(h);
        log.debug("[UserAccountHistoryService] 이력 기록 - type: {}, target: {}, by: {}",
                actionType, targetUsername, performedBy);
    }

    /**
     * 필터 조건으로 이력 목록을 페이징 조회합니다.
     *
     * @param actionType 처리 유형 필터 (null=전체)
     * @param username   대상 사용자 계정 키워드 (null=전체)
     * @param dateFrom   시작 날짜 (null=전체)
     * @param dateTo     종료 날짜 (null=전체)
     * @param pageable   페이징 정보
     */
    @Transactional(readOnly = true)
    public Page<UserAccountHistory> findByFilter(String actionType, String username,
                                                  LocalDate dateFrom, LocalDate dateTo,
                                                  Pageable pageable) {
        LocalDateTime from = (dateFrom != null) ? dateFrom.atStartOfDay() : null;
        LocalDateTime to   = (dateTo   != null) ? dateTo.atTime(LocalTime.MAX) : null;

        String actionTypeParam = (actionType != null && !actionType.isBlank()) ? actionType : null;
        String usernameParam   = (username   != null && !username.isBlank())   ? username   : null;

        return repository.findByFilter(actionTypeParam, usernameParam, from, to, pageable);
    }
}
