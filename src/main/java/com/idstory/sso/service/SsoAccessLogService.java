package com.idstory.sso.service;

import com.idstory.common.util.OidGenerator;
import com.idstory.sso.entity.SsoAccessLog;
import com.idstory.sso.repository.SsoAccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SSO 접근 이력 기록/조회 서비스
 * - @Async 비동기 기록: 메인 트랜잭션에 영향 없음
 */
@Service
@RequiredArgsConstructor
public class SsoAccessLogService {

    private final SsoAccessLogRepository accessLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String clientOid, String clientId, String userOid, String userId,
                    String ip, String action, String status, String detail) {
        SsoAccessLog log = SsoAccessLog.builder()
                .logOid(OidGenerator.generate())
                .clientOid(clientOid)
                .clientId(clientId)
                .userOid(userOid)
                .userId(userId)
                .ipAddress(ip)
                .action(action)
                .status(status)
                .detail(detail)
                .accessedAt(LocalDateTime.now())
                .build();
        accessLogRepository.save(log);
    }

    public Page<SsoAccessLog> findByFilter(String clientId, String userId, String action,
                                           String status, String ipAddress,
                                           LocalDateTime dateFrom, LocalDateTime dateTo,
                                           Pageable pageable) {
        return accessLogRepository.findByFilter(
                blank(clientId)   ? null : clientId,
                blank(userId)     ? null : userId,
                blank(action)     ? null : action,
                blank(status)     ? null : status,
                blank(ipAddress)  ? null : ipAddress,
                dateFrom, dateTo, pageable);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
