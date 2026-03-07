package com.idstory.user.service;

import com.idstory.common.security.CustomPasswordEncoder;
import com.idstory.common.util.OidGenerator;
import com.idstory.history.service.UserAccountHistoryService;
import com.idstory.policy.service.PasswordPolicyService;
import com.idstory.policy.service.SystemPolicyService;
import com.idstory.user.dto.UserCreateDto;
import com.idstory.user.dto.UserUpdateDto;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 시스템 사용자 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepository sysUserRepository;
    private final CustomPasswordEncoder passwordEncoder;
    private final UserAccountHistoryService historyService;
    private final PasswordPolicyService passwordPolicyService;
    private final SystemPolicyService systemPolicyService;

    /**
     * 신규 사용자를 등록합니다.
     *
     * <p>비밀번호 저장 규칙:</p>
     * <ul>
     *   <li>password 컬럼      = HASH(salt? + 입력 비밀번호) — 순수 해시</li>
     *   <li>password_salt 컬럼 = XML password-salt 값 (enabled=true 시), null (false 시)</li>
     * </ul>
     *
     * @param dto         사용자 등록 DTO
     * @param performedBy 처리자 user_id (현재 로그인 사용자)
     * @param ip          처리자 IP 주소
     * @throws IllegalArgumentException user_id 또는 email 중복 시
     */
    @Transactional
    public SysUser createUser(UserCreateDto dto, String performedBy, String ip) {
        // 사용자 ID 정책 검증
        validateUserId(dto.getUserId());

        if (sysUserRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다: " + dto.getUserId());
        }
        if (sysUserRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + dto.getEmail());
        }

        // 비밀번호 정책 검증
        validatePassword(dto.getPassword());

        String oid  = OidGenerator.generate();
        String hash = passwordEncoder.encode(dto.getPassword());
        String salt = passwordEncoder.getConfiguredSalt(); // null when disabled

        SysUser user = SysUser.builder()
                .oid(oid)
                .userId(dto.getUserId())
                .name(dto.getName())
                .password(hash)
                .passwordSalt(salt)
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .deptCode(blankToNull(dto.getDeptCode()))
                .role(dto.getRole() != null ? dto.getRole() : "USER")
                .useYn("Y")
                .status("ACTIVE")
                .concurrentYn(dto.getConcurrentYn() != null ? dto.getConcurrentYn() : "N")
                .validStartDate(dto.getValidStartDate())
                .validEndDate(dto.getValidEndDate())
                .jobDuty(blankToNull(dto.getJobDuty()))
                .createdBy(performedBy)
                .build();

        sysUserRepository.save(user);

        String detail = String.format("사용자 등록 - userId: %s, name: %s, role: %s",
                user.getUserId(), user.getName(), user.getRole());
        historyService.log(oid, user.getUserId(), "CREATE", detail, performedBy, ip);

        log.info("[SysUserService] 사용자 등록 완료 - oid: {}, userId: {}", oid, user.getUserId());
        return user;
    }

    /**
     * OID로 사용자 단건 조회 (수정 모달에서 사용)
     *
     * @param oid 사용자 OID
     * @throws IllegalArgumentException 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public SysUser getUserByOid(String oid) {
        return sysUserRepository.findById(oid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + oid));
    }

    /**
     * 사용자 정보를 수정합니다.
     *
     * <ul>
     *   <li>이메일 변경 시 중복 검사</li>
     *   <li>잠금 해제(Y→N) 시 loginFailCount 자동 초기화</li>
     *   <li>newPassword 입력 시 비밀번호 재설정</li>
     * </ul>
     *
     * @param oid         대상 사용자 OID
     * @param dto         수정 DTO
     * @param performedBy 처리자 user_id
     * @param ip          처리자 IP
     */
    @Transactional
    public SysUser updateUser(String oid, UserUpdateDto dto, String performedBy, String ip) {
        SysUser user = getUserByOid(oid);

        if (!dto.getEmail().equals(user.getEmail())
                && sysUserRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + dto.getEmail());
        }

        boolean isUnlocking = "Y".equals(user.getLockYn()) && "N".equals(dto.getLockYn());

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(blankToNull(dto.getPhone()));
        user.setDeptCode(blankToNull(dto.getDeptCode()));
        user.setRole(dto.getRole() != null ? dto.getRole() : "USER");
        user.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        user.setLockYn(dto.getLockYn() != null ? dto.getLockYn() : "N");
        if (isUnlocking) {
            user.setLoginFailCount(0);
            user.setLockedAt(null);
        }
        user.setConcurrentYn(dto.getConcurrentYn() != null ? dto.getConcurrentYn() : "N");
        user.setValidStartDate(dto.getValidStartDate());
        user.setValidEndDate(dto.getValidEndDate());
        user.setJobDuty(blankToNull(dto.getJobDuty()));
        user.setUpdatedBy(performedBy);

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            String hash = passwordEncoder.encode(dto.getNewPassword());
            String salt = passwordEncoder.getConfiguredSalt();
            user.setPassword(hash);
            user.setPasswordSalt(salt);
        }

        sysUserRepository.save(user);

        String detail = String.format(
                "사용자 수정 - userId: %s, name: %s, role: %s, status: %s, lockYn: %s",
                user.getUserId(), user.getName(), user.getRole(),
                user.getStatus(), user.getLockYn());
        historyService.log(oid, user.getUserId(), "UPDATE", detail, performedBy, ip);

        log.info("[SysUserService] 사용자 수정 완료 - oid: {}, userId: {}", oid, user.getUserId());
        return user;
    }

    /**
     * 다중 조건 필터로 사용자 목록을 페이징 조회합니다.
     *
     * @param keyword  아이디/이름 키워드 (null 또는 빈 문자열=전체)
     * @param deptCode 부서코드 (null=전체)
     * @param role     역할 (null=전체)
     * @param useYn    사용여부 (null=전체)
     * @param status   상태 (null=전체)
     * @param pageable 페이징 정보
     */
    @Transactional(readOnly = true)
    public Page<SysUser> findUsers(String keyword, String deptCode, String role,
                                   String useYn, String status, String lockYn,
                                   Pageable pageable) {
        return sysUserRepository.findByFilter(
                blankToNull(keyword),
                blankToNull(deptCode),
                blankToNull(role),
                blankToNull(useYn),
                blankToNull(status),
                blankToNull(lockYn),
                pageable);
    }

    /**
     * 로그인 성공 시 연속 실패 횟수를 초기화합니다.
     *
     * @param userId 로그인한 계정
     */
    @Transactional
    public void handleLoginSuccess(String userId) {
        sysUserRepository.findByUserId(userId).ifPresent(user -> {
            if (user.getLoginFailCount() > 0) {
                user.setLoginFailCount(0);
                sysUserRepository.save(user);
                log.debug("[SysUserService] 로그인 성공 - 실패 횟수 초기화 - userId: {}", userId);
            }
        });
    }

    /**
     * 로그인 실패(비밀번호 불일치) 시 실패 횟수를 증가시키고,
     * 정책 초과 시 계정을 자동 잠금합니다.
     *
     * @param userId 로그인 시도 계정
     * @param ip     클라이언트 IP
     */
    @Transactional
    public void handleLoginFailure(String userId, String ip) {
        sysUserRepository.findByUserId(userId).ifPresent(user -> {
            // 이미 잠긴 계정은 카운트 증가 불필요
            if ("Y".equals(user.getLockYn())) return;

            int newCount = user.getLoginFailCount() + 1;
            user.setLoginFailCount(newCount);

            // 역할별 정책 분기 (ADMIN vs USER)
            boolean isAdmin = "ADMIN".equals(user.getRole());
            int maxFail = isAdmin
                    ? systemPolicyService.getInt("ADMIN_POLICY", "ADMIN_MAX_LOGIN_FAIL", 5)
                    : systemPolicyService.getInt("PASSWORD_POLICY", "MAX_LOGIN_FAIL_COUNT", 5);
            boolean lockEnabled = systemPolicyService.getBoolean("PASSWORD_POLICY", "PWD_MAX_FAIL_LOCK", true);

            if (lockEnabled && newCount >= maxFail) {
                user.setLockYn("Y");
                user.setLockedAt(LocalDateTime.now());
                sysUserRepository.save(user);

                historyService.log(user.getOid(), userId, "LOCK",
                        "비밀번호 " + newCount + "회 연속 실패로 계정 자동 잠금 (정책: " + maxFail + "회)",
                        "SYSTEM", ip);
                log.warn("[SysUserService] 계정 자동 잠금 - userId: {}, 실패: {}회 (정책: {}회)",
                        userId, newCount, maxFail);
            } else {
                sysUserRepository.save(user);
                log.warn("[SysUserService] 로그인 실패 - userId: {}, 누적: {}회 / 최대: {}회",
                        userId, newCount, maxFail);
            }
        });
    }

    /**
     * 사용자 ID 정책 검증
     */
    public void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 ID를 입력해주세요.");
        }
        int minLen = systemPolicyService.getInt("USER_POLICY", "USER_ID_MIN_LEN", 4);
        int maxLen = systemPolicyService.getInt("USER_POLICY", "USER_ID_MAX_LEN", 20);
        boolean requireLetter = systemPolicyService.getBoolean("USER_POLICY", "USER_ID_REQUIRE_LETTER", true);
        boolean startLetter   = systemPolicyService.getBoolean("USER_POLICY", "USER_ID_START_LETTER", false);
        String  regex         = systemPolicyService.getString("USER_POLICY", "USER_ID_REGEX", "");

        if (userId.length() < minLen) {
            throw new IllegalArgumentException("사용자 ID는 " + minLen + "자 이상이어야 합니다.");
        }
        if (userId.length() > maxLen) {
            throw new IllegalArgumentException("사용자 ID는 " + maxLen + "자 이하이어야 합니다.");
        }
        if (requireLetter && !userId.chars().anyMatch(Character::isLetter)) {
            throw new IllegalArgumentException("사용자 ID에 영문자가 포함되어야 합니다.");
        }
        if (startLetter && !Character.isLetter(userId.charAt(0))) {
            throw new IllegalArgumentException("사용자 ID는 영문자로 시작해야 합니다.");
        }
        if (regex != null && !regex.isBlank()) {
            if (!userId.matches(regex)) {
                throw new IllegalArgumentException("사용자 ID 형식이 올바르지 않습니다. (규칙: " + regex + ")");
            }
        }
    }

    /**
     * 비밀번호 정책 검증
     */
    public void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        int minLen = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_MIN_LEN", 8);
        int maxLen = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_MAX_LEN", 100);
        int minUpper   = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_MIN_UPPER",   0);
        int minLower   = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_MIN_LOWER",   0);
        int minDigit   = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_MIN_DIGIT",   0);
        int minSpecial = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_MIN_SPECIAL", 0);
        int maxRepeat  = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_MAX_REPEAT",  0);
        boolean noConsecutive  = systemPolicyService.getBoolean("PASSWORD_POLICY", "PWD_NO_CONSECUTIVE", false);
        String forbiddenWords  = systemPolicyService.getString("PASSWORD_POLICY", "PWD_FORBIDDEN_WORDS", "");
        String allowedSpecials = systemPolicyService.getString("PASSWORD_POLICY", "PWD_ALLOWED_SPECIAL", "!@#$%^&*()_+-=");

        if (password.length() < minLen) {
            throw new IllegalArgumentException("비밀번호는 " + minLen + "자 이상이어야 합니다.");
        }
        if (password.length() > maxLen) {
            throw new IllegalArgumentException("비밀번호는 " + maxLen + "자 이하이어야 합니다.");
        }
        if (minUpper > 0 && password.chars().filter(Character::isUpperCase).count() < minUpper) {
            throw new IllegalArgumentException("비밀번호에 대문자가 " + minUpper + "자 이상 포함되어야 합니다.");
        }
        if (minLower > 0 && password.chars().filter(Character::isLowerCase).count() < minLower) {
            throw new IllegalArgumentException("비밀번호에 소문자가 " + minLower + "자 이상 포함되어야 합니다.");
        }
        if (minDigit > 0 && password.chars().filter(Character::isDigit).count() < minDigit) {
            throw new IllegalArgumentException("비밀번호에 숫자가 " + minDigit + "자 이상 포함되어야 합니다.");
        }
        if (minSpecial > 0) {
            long specialCount = password.chars().filter(c -> allowedSpecials.indexOf(c) >= 0).count();
            if (specialCount < minSpecial) {
                throw new IllegalArgumentException("비밀번호에 특수문자(" + allowedSpecials + ")가 " + minSpecial + "자 이상 포함되어야 합니다.");
            }
        }
        if (maxRepeat > 0) {
            int consecutive = 1;
            for (int i = 1; i < password.length(); i++) {
                if (password.charAt(i) == password.charAt(i - 1)) {
                    consecutive++;
                    if (consecutive > maxRepeat) {
                        throw new IllegalArgumentException("동일 문자를 " + maxRepeat + "자 초과 연속 사용할 수 없습니다.");
                    }
                } else {
                    consecutive = 1;
                }
            }
        }
        if (noConsecutive) {
            for (int i = 0; i < password.length() - 2; i++) {
                int c1 = password.charAt(i), c2 = password.charAt(i + 1), c3 = password.charAt(i + 2);
                if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                    throw new IllegalArgumentException("연속된 문자(abc, 123 등)는 사용할 수 없습니다.");
                }
            }
        }
        if (forbiddenWords != null && !forbiddenWords.isBlank()) {
            String lowerPwd = password.toLowerCase();
            Arrays.stream(forbiddenWords.split(","))
                    .map(String::trim)
                    .filter(w -> !w.isBlank())
                    .forEach(word -> {
                        if (lowerPwd.contains(word.toLowerCase())) {
                            throw new IllegalArgumentException("비밀번호에 금지 단어(" + word + ")를 사용할 수 없습니다.");
                        }
                    });
        }
    }

    /**
     * 초기 비밀번호를 정책에 따라 반환합니다 (FIXED/RANDOM).
     */
    public String getInitialPassword() {
        String type = systemPolicyService.getString("PASSWORD_POLICY", "PWD_RESET_TYPE", "FIXED");
        if ("RANDOM".equalsIgnoreCase(type)) {
            int saltLen = systemPolicyService.getInt("PASSWORD_POLICY", "PWD_SALT_LEN", 8);
            return OidGenerator.randomAlphanumeric(Math.max(saltLen, 8));
        }
        return systemPolicyService.getString("PASSWORD_POLICY", "PWD_RESET_VALUE", "1234");
    }

    /**
     * 관리자 등록 모달용: 일반 사용자(role=USER) 키워드 검색 (최대 20건)
     *
     * @param keyword 아이디/이름 키워드 (null=전체)
     */
    @Transactional(readOnly = true)
    public List<SysUser> searchNonAdminUsers(String keyword) {
        return sysUserRepository.findNonAdminUsers(
                blankToNull(keyword),
                PageRequest.of(0, 20));
    }

    /**
     * 특정 부서의 사용자 목록을 페이징 조회합니다 (조직사용자 화면용).
     */
    @Transactional(readOnly = true)
    public Page<SysUser> findUsersByDept(String deptCode, String keyword, Pageable pageable) {
        return sysUserRepository.findByDeptCode(deptCode, blankToNull(keyword), pageable);
    }

    /**
     * 배정 모달용: 전체 활성 사용자 키워드 검색 (페이징).
     */
    @Transactional(readOnly = true)
    public Page<SysUser> searchAssignableUsers(String keyword, Pageable pageable) {
        return sysUserRepository.findAssignableUsers(blankToNull(keyword), pageable);
    }

    /**
     * 사용자를 특정 부서에 배정합니다 (부서 코드 변경).
     *
     * @param userOid     대상 사용자 OID
     * @param deptCode    배정할 부서코드 (null = 부서 해제)
     * @param performedBy 처리자
     * @param ip          처리자 IP
     */
    @Transactional
    public void assignDept(String userOid, String deptCode, String performedBy, String ip) {
        SysUser user = getUserByOid(userOid);
        String prevDept = user.getDeptCode();
        user.setDeptCode(blankToNull(deptCode));
        user.setUpdatedBy(performedBy);
        sysUserRepository.save(user);

        String detail = String.format("부서 변경 - %s → %s",
                prevDept != null ? prevDept : "(없음)",
                deptCode != null && !deptCode.isBlank() ? deptCode : "(없음)");
        historyService.log(userOid, user.getUserId(), "UPDATE", detail, performedBy, ip);
        log.info("[SysUserService] 부서 배정 - oid: {}, {} → {}", userOid, prevDept, deptCode);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
