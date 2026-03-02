package com.idstory.login.service;

import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Security UserDetailsService 구현체
 * sys_users 테이블에서 사용자 정보를 조회하여 인증에 사용합니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository sysUserRepository;

    public CustomUserDetailsService(SysUserRepository sysUserRepository) {
        this.sysUserRepository = sysUserRepository;
    }

    /**
     * 로그인 계정(user_id)으로 UserDetails를 로드합니다.
     *
     * @param userId 로그인 계정 (sys_users.user_id)
     * @return UserDetails (Spring Security 인증 객체)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.debug("[CustomUserDetailsService] 사용자 조회 시작 - userId: {}", userId);

        SysUser user = sysUserRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[CustomUserDetailsService] 사용자 없음 - userId: {}", userId);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
                });

        boolean disabled       = !"Y".equals(user.getUseYn());
        boolean accountLocked  = "Y".equals(user.getLockYn());
        boolean accountExpired = user.getValidEndDate() != null
                && LocalDate.now().isAfter(user.getValidEndDate());

        log.info("[CustomUserDetailsService] 사용자 조회 완료 - userId: {}, role: {}, useYn: {}, status: {}",
                user.getUserId(), user.getRole(), user.getUseYn(), user.getStatus());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .disabled(disabled)
                .accountExpired(accountExpired)
                .credentialsExpired(false)
                .accountLocked(accountLocked)
                .build();
    }
}
