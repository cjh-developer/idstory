package com.idstory.password.service;

import com.idstory.common.security.CustomPasswordEncoder;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import com.idstory.password.entity.PasswordResetToken;
import com.idstory.password.repository.PasswordResetTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비밀번호 초기화 서비스
 *
 * <p>흐름</p>
 * <ol>
 *   <li>사용자가 아이디 + 이메일 입력</li>
 *   <li>일치하는 계정 확인 → UUID 토큰 생성 (24시간 유효)</li>
 *   <li>토큰을 DB 저장 후 초기화 URL 반환</li>
 *   <li>사용자가 URL 접근 → 새 비밀번호 입력 → 비밀번호 변경</li>
 * </ol>
 *
 * <p>※ 실제 운영 환경에서는 토큰 URL을 화면에 노출하지 않고
 * JavaMailSender 등을 통해 이메일로 발송해야 합니다.</p>
 */
@Slf4j
@Service
@Transactional
public class PasswordResetService {

    /** 토큰 유효 시간 (시간 단위) */
    private static final int EXPIRY_HOURS = 24;

    private final SysUserRepository sysUserRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final CustomPasswordEncoder passwordEncoder;

    public PasswordResetService(SysUserRepository sysUserRepository,
                                PasswordResetTokenRepository tokenRepository,
                                CustomPasswordEncoder passwordEncoder) {
        this.sysUserRepository = sysUserRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 비밀번호 초기화 토큰을 생성합니다.
     *
     * @param userId 로그인 계정 (sys_users.user_id)
     * @param email  이메일 (계정 확인용)
     * @return 생성된 UUID 토큰 문자열
     * @throws IllegalArgumentException 아이디/이메일 불일치 시
     */
    public String createResetToken(String userId, String email) {
        SysUser user = sysUserRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[PasswordResetService] 존재하지 않는 계정 초기화 시도 - userId: {}", userId);
                    return new IllegalArgumentException("reset.error.notFound");
                });

        if (user.getEmail() == null || !email.trim().equalsIgnoreCase(user.getEmail())) {
            log.warn("[PasswordResetService] 이메일 불일치 - userId: {}", userId);
            throw new IllegalArgumentException("reset.error.notFound");
        }

        if (!"Y".equals(user.getUseYn())) {
            log.warn("[PasswordResetService] 비활성화 계정 초기화 시도 - userId: {}", userId);
            throw new IllegalArgumentException("reset.error.disabled");
        }

        // 기존 미사용 토큰 제거 (중복 발급 방지)
        tokenRepository.deleteUnusedByUsername(userId);

        String tokenValue = UUID.randomUUID().toString();

        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .username(userId)
                .expiryDate(LocalDateTime.now().plusHours(EXPIRY_HOURS))
                .used(false)
                .build();

        tokenRepository.save(token);

        log.info("[PasswordResetService] 초기화 토큰 생성 완료 - userId: {}, 만료: {}시간 후",
                userId, EXPIRY_HOURS);
        return tokenValue;
    }

    /**
     * 토큰으로 비밀번호를 변경합니다.
     *
     * @param tokenValue  초기화 토큰 (UUID)
     * @param newPassword 새 비밀번호 (평문)
     * @throws IllegalArgumentException 유효하지 않은 토큰
     */
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> {
                    log.warn("[PasswordResetService] 존재하지 않는 토큰으로 비밀번호 변경 시도");
                    return new IllegalArgumentException("reset.error.invalid");
                });

        if (token.isExpired()) {
            log.warn("[PasswordResetService] 만료된 토큰 사용 시도 - userId: {}", token.getUsername());
            throw new IllegalArgumentException("reset.error.expired");
        }
        if (token.isUsed()) {
            log.warn("[PasswordResetService] 이미 사용된 토큰 재사용 시도 - userId: {}", token.getUsername());
            throw new IllegalArgumentException("reset.error.used");
        }

        SysUser user = sysUserRepository.findByUserId(token.getUsername())
                .orElseThrow(() -> {
                    log.warn("[PasswordResetService] 토큰에 연결된 사용자 없음 - userId: {}", token.getUsername());
                    return new IllegalArgumentException("reset.error.invalid");
                });

        String hash = passwordEncoder.encode(newPassword);
        user.setPassword(hash);
        user.setPasswordSalt(passwordEncoder.getConfiguredSalt());
        sysUserRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("[PasswordResetService] 비밀번호 변경 완료 - userId: {}", user.getUserId());
    }

    /**
     * 토큰의 유효성을 검증합니다 (변경 없음, 조회 전용).
     *
     * @param tokenValue UUID 토큰
     * @return 유효한 토큰 엔티티
     * @throws IllegalArgumentException 유효하지 않은 토큰
     */
    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String tokenValue) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> {
                    log.warn("[PasswordResetService] 토큰 검증 실패 - 토큰 없음");
                    return new IllegalArgumentException("reset.error.invalid");
                });

        if (!token.isValid()) {
            String reason = token.isExpired() ? "reset.error.expired" : "reset.error.used";
            log.warn("[PasswordResetService] 토큰 검증 실패 - userId: {}, 사유: {}",
                    token.getUsername(), reason);
            throw new IllegalArgumentException(reason);
        }
        log.debug("[PasswordResetService] 토큰 검증 성공 - userId: {}", token.getUsername());
        return token;
    }
}
