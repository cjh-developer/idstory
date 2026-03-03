package com.idstory.menu.service;

import com.idstory.menu.entity.Menu;
import com.idstory.menu.repository.MenuRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 시스템 메뉴 서비스
 *
 * <ul>
 *   <li>사이드바 렌더링용 트리 조회 (역할 기반 필터링)</li>
 *   <li>관리 페이지용 전체 목록 조회</li>
 *   <li>메뉴 추가 / 삭제 / 활성화 토글 / 역할 수정</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    // ── 사이드바용 ──────────────────────────────────────────────────────────

    /**
     * 역할 기반으로 필터링된 메뉴 트리를 반환합니다.
     *
     * <p>필터 규칙:</p>
     * <ul>
     *   <li>menu.roles 가 비어있으면 → 모든 인증 사용자에게 표시</li>
     *   <li>menu.roles 에 값이 있으면 → userRoles 와 교집합이 있는 경우만 표시</li>
     * </ul>
     *
     * @param userRoles 현재 로그인 사용자의 권한 집합 (예: ["ADMIN"])
     */
    public List<Menu> getMenuTreeByRoles(Set<String> userRoles) {
        List<Menu> all = menuRepository.findEnabledWithRoles();

        List<Menu> filtered = all.stream()
                .filter(m -> m.getRoles().isEmpty()
                        || m.getRoles().stream().anyMatch(userRoles::contains))
                .collect(Collectors.toList());

        return buildTree(filtered);
    }

    // ── 관리 페이지용 ───────────────────────────────────────────────────────

    /**
     * 전체 메뉴 목록 (비활성 포함, roles JOIN FETCH)
     */
    public List<Menu> getAllMenus() {
        return menuRepository.findAllWithRoles();
    }

    /**
     * 최상위 메뉴 목록 — 하위 메뉴 추가 시 부모 드롭다운에 사용
     */
    public List<Menu> getTopMenus() {
        return menuRepository.findByParentIdIsNullOrderBySortOrderAsc();
    }

    /**
     * 메뉴 ID → 메뉴명 맵 (관리 페이지 테이블의 '상위 메뉴' 열 표시용)
     */
    public Map<Long, String> getMenuNameMap() {
        return menuRepository.findAllWithRoles().stream()
                .collect(Collectors.toMap(
                        Menu::getMenuId, Menu::getMenuName,
                        (a, b) -> a, LinkedHashMap::new));
    }

    // ── 변경 작업 ───────────────────────────────────────────────────────────

    /** 메뉴 활성화/비활성화 토글 (잠금 메뉴는 불가) */
    @Transactional
    public void toggleEnabled(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴: " + menuId));
        if (menu.isLocked()) {
            throw new IllegalStateException("잠금된 메뉴는 변경할 수 없습니다: " + menu.getMenuName());
        }
        menu.setEnabled(!menu.isEnabled());
        log.info("[MenuService] 메뉴 활성화 토글 - id: {}, name: {}, enabled: {}",
                menu.getMenuId(), menu.getMenuName(), menu.isEnabled());
    }

    /** 새 메뉴 저장 */
    @Transactional
    public void save(Menu menu) {
        menuRepository.save(menu);
        log.info("[MenuService] 메뉴 추가 - name: {}, parentId: {}", menu.getMenuName(), menu.getParentId());
    }

    /**
     * 메뉴 삭제 (잠금 메뉴 및 하위가 있는 메뉴는 불가)
     */
    @Transactional
    public void delete(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴: " + menuId));
        if (menu.isLocked()) {
            throw new IllegalStateException("잠금된 메뉴는 삭제할 수 없습니다: " + menu.getMenuName());
        }
        if (menuRepository.existsByParentId(menuId)) {
            throw new IllegalStateException("하위 메뉴가 있는 메뉴는 삭제할 수 없습니다. 먼저 하위 메뉴를 삭제하세요.");
        }
        menuRepository.delete(menu);
        log.info("[MenuService] 메뉴 삭제 - id: {}, name: {}", menuId, menu.getMenuName());
    }

    /**
     * 메뉴 역할 업데이트 (roles 전체 교체)
     *
     * @param menuId 대상 메뉴 ID
     * @param roles  새로 설정할 역할 집합 (빈 Set = 전체 공개)
     */
    @Transactional
    public void updateMenu(Long menuId, String menuName, String icon, String url) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴: " + menuId));
        if (menu.isLocked()) {
            throw new IllegalStateException("잠금된 메뉴는 수정할 수 없습니다.");
        }
        menu.setMenuName(menuName.trim());
        menu.setIcon(icon == null || icon.isBlank() ? null : icon.trim());
        menu.setUrl(url == null || url.isBlank() ? null : url.trim());
        log.info("[MenuService] 메뉴 수정 - id: {}, name: {}", menuId, menuName);
    }

    @Transactional
    public void updateRoles(Long menuId, Set<String> roles) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴: " + menuId));
        if (menu.isLocked()) {
            throw new IllegalStateException("잠금된 메뉴의 역할은 변경할 수 없습니다.");
        }
        menu.getRoles().clear();
        menu.getRoles().addAll(roles);
        log.info("[MenuService] 메뉴 역할 수정 - id: {}, name: {}, roles: {}",
                menuId, menu.getMenuName(), roles);
    }

    // ── 내부 유틸 ───────────────────────────────────────────────────────────

    private List<Menu> buildTree(List<Menu> flat) {
        Map<Long, Menu> map = new LinkedHashMap<>();
        flat.forEach(m -> {
            m.setChildren(new ArrayList<>());
            map.put(m.getMenuId(), m);
        });
        List<Menu> roots = new ArrayList<>();
        for (Menu m : flat) {
            if (m.getParentId() == null) {
                roots.add(m);
            } else {
                Menu parent = map.get(m.getParentId());
                if (parent != null) parent.getChildren().add(m);
            }
        }
        return roots;
    }
}
