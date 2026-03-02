package com.idstory.history.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.history.entity.LoginHistory;
import com.idstory.history.repository.LoginHistoryRepository;
import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 로그인 이력 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final SysUserRepository sysUserRepository;

    /**
     * 로그인 이벤트 기록
     *
     * @param userId     로그인 시도 계정
     * @param actionType LOGIN_SUCCESS | LOGIN_FAIL | LOGOUT
     * @param failReason 실패 사유 (실패 시에만, 그 외 null)
     * @param ip         클라이언트 IP
     */
    @Transactional
    public void log(String userId, String actionType, String failReason, String ip) {
        String userOid = sysUserRepository.findByUserId(userId)
                .map(u -> u.getOid())
                .orElse(null);

        LoginHistory history = LoginHistory.builder()
                .histOid(OidGenerator.generate())
                .userOid(userOid)
                .userId(userId)
                .actionType(actionType)
                .failReason(failReason)
                .ipAddress(ip)
                .performedAt(LocalDateTime.now())
                .build();

        loginHistoryRepository.save(history);
    }

    /**
     * 다중 조건 필터로 로그인 이력을 페이징 조회합니다.
     *
     * @param userId     아이디 키워드 (null=전체)
     * @param actionType 이벤트 유형 (null=전체)
     * @param dateFrom   조회 시작 일자 문자열 "yyyy-MM-dd" (null=전체)
     * @param dateTo     조회 종료 일자 문자열 "yyyy-MM-dd" (null=전체)
     * @param pageable   페이징 정보
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> findHistory(String userId, String actionType,
                                          String dateFrom, String dateTo,
                                          Pageable pageable) {
        LocalDateTime from = (dateFrom != null && !dateFrom.isBlank())
                ? LocalDateTime.parse(dateFrom + "T00:00:00") : null;
        LocalDateTime to = (dateTo != null && !dateTo.isBlank())
                ? LocalDateTime.parse(dateTo + "T23:59:59") : null;

        return loginHistoryRepository.findByFilter(
                blankToNull(userId),
                blankToNull(actionType),
                from, to,
                pageable);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
