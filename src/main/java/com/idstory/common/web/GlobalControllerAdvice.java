package com.idstory.common.web;

import com.idstory.menu.entity.Menu;
import com.idstory.menu.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 전역 컨트롤러 어드바이스
 *
 * <p>모든 컨트롤러 실행 전 공통 모델 속성을 주입합니다.</p>
 * <ul>
 *   <li>{@code menuTree} — 현재 사용자 역할로 필터링된 활성 메뉴 트리</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    private final MenuService menuService;

    public GlobalControllerAdvice(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 현재 로그인 사용자의 역할에 맞는 메뉴 트리를 주입합니다.
     *
     * <p>역할 추출 규칙: Spring Security GrantedAuthority 에서 "ROLE_" 접두사를 제거합니다.</p>
     * <p>DB 오류 시 빈 리스트를 반환하여 페이지 렌더링을 보호합니다.</p>
     */
    @ModelAttribute("menuTree")
    public List<Menu> menuTree(Authentication auth) {
        try {
            Set<String> roles = Collections.emptySet();
            if (auth != null && auth.isAuthenticated()) {
                roles = auth.getAuthorities().stream()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .collect(Collectors.toSet());
            }
            return menuService.getMenuTreeByRoles(roles);
        } catch (Exception e) {
            log.warn("[GlobalControllerAdvice] 메뉴 트리 조회 실패 - {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
