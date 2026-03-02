package com.idstory.common.security;

import com.idstory.history.service.LoginHistoryService;
import com.idstory.login.service.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security 설정 클래스
 *
 * <ul>
 *   <li>폼 로그인 설정</li>
 *   <li>URL 접근 권한 설정</li>
 *   <li>커스텀 비밀번호 인코더 연결 (CustomPasswordEncoder)</li>
 *   <li>로그아웃 설정</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomPasswordEncoder passwordEncoder;
    private final LoginHistoryService loginHistoryService;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          CustomPasswordEncoder passwordEncoder,
                          LoginHistoryService loginHistoryService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginHistoryService = loginHistoryService;
    }

    /**
     * HTTP 보안 필터 체인 설정
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── URL 접근 권한 ─────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login",
                    "/password-reset",
                    "/password-reset/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/font/**",
                    "/favicon.ico"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // ── 폼 로그인 설정 ────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")                    // 커스텀 로그인 페이지
                .loginProcessingUrl("/login")           // 폼 action URL
                .usernameParameter("username")          // 폼 필드명
                .passwordParameter("password")          // 폼 필드명
                .defaultSuccessUrl("/", true)           // 로그인 성공 후 이동
                .failureUrl("/login?error=true")        // 로그인 실패 후 이동
                .permitAll()
            )

            // ── 로그아웃 설정 ─────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessHandler(loggingLogoutSuccessHandler()) // 로그 + 리다이렉트
                .invalidateHttpSession(true)            // 세션 무효화
                .deleteCookies("JSESSIONID")            // 쿠키 삭제
                .clearAuthentication(true)
                .permitAll()
            )

            // ── 인증 공급자 연결 ──────────────────────────────────────────────
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    /**
     * DAO 기반 인증 공급자 설정
     * - UserDetailsService: CustomUserDetailsService (DB 조회)
     * - PasswordEncoder: CustomPasswordEncoder (SHA-256/512 + HEX/BASE64)
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * 로그아웃 성공 핸들러 — 로그 기록 후 로그인 페이지로 리다이렉트
     */
    @Bean
    public LogoutSuccessHandler loggingLogoutSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication auth) -> {
            if (auth != null) {
                String ip = request.getRemoteAddr();
                log.info("[AUTH] 로그아웃 - username: {}, IP: {}", auth.getName(), ip);
                loginHistoryService.log(auth.getName(), "LOGOUT", null, ip);
            }
            SimpleUrlLogoutSuccessHandler delegate = new SimpleUrlLogoutSuccessHandler();
            delegate.setDefaultTargetUrl("/login?logout=true");
            delegate.onLogoutSuccess(request, response, auth);
        };
    }

    /**
     * AuthenticationManager 빈 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
