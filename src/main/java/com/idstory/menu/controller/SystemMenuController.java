package com.idstory.menu.controller;

import com.idstory.menu.entity.Menu;
import com.idstory.menu.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 시스템 설정 > 메뉴 관리 컨트롤러 (ADMIN 전용)
 *
 * <ul>
 *   <li>GET  /system/menu              — 메뉴 목록</li>
 *   <li>POST /system/menu              — 메뉴 추가</li>
 *   <li>POST /system/menu/{id}/toggle  — 활성화 토글</li>
 *   <li>POST /system/menu/{id}/delete  — 삭제</li>
 *   <li>POST /system/menu/{id}/roles   — 역할 수정</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/system/menu")
@PreAuthorize("hasRole('ADMIN')")
public class SystemMenuController {

    /** 시스템에서 사용 가능한 권한 목록 */
    private static final List<String> AVAILABLE_ROLES = List.of("USER", "ADMIN");

    private final MenuService menuService;

    public SystemMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ── 목록 ────────────────────────────────────────────────────────────────

    @GetMapping
    public String list(Model model, Authentication auth) {
        log.info("[SystemMenuController] 메뉴 관리 접근 - username: {}",
                auth != null ? auth.getName() : "unknown");

        List<Menu> allMenus = menuService.getAllMenus();

        // 메뉴 ID → roles 문자열 맵 (template의 data-roles 속성용)
        Map<Long, String> menuRolesStr = allMenus.stream()
                .collect(Collectors.toMap(
                        Menu::getMenuId,
                        m -> String.join(",", m.getRoles())));

        model.addAttribute("allMenus",      allMenus);
        model.addAttribute("topMenus",      menuService.getTopMenus());
        model.addAttribute("menuNameMap",   menuService.getMenuNameMap());
        model.addAttribute("menuRolesStr",  menuRolesStr);
        model.addAttribute("availableRoles", AVAILABLE_ROLES);
        return "main/system/menu";
    }

    // ── 추가 ────────────────────────────────────────────────────────────────

    @PostMapping
    public String create(
            @RequestParam String menuName,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String url,
            @RequestParam int sortOrder,
            @RequestParam(required = false) List<String> roles,
            Authentication auth,
            RedirectAttributes ra) {

        log.info("[SystemMenuController] 메뉴 추가 - name: {}, parentId: {}, roles: {}, by: {}",
                menuName, parentId, roles, auth != null ? auth.getName() : "unknown");

        Menu menu = Menu.builder()
                .menuName(menuName.trim())
                .parentId(parentId)
                .icon(icon != null && !icon.isBlank() ? icon.trim() : null)
                .url(url != null && !url.isBlank() ? url.trim() : null)
                .sortOrder(sortOrder)
                .enabled(true)
                .locked(false)
                .build();

        // roles 설정
        if (roles != null) {
            menu.getRoles().addAll(roles);
        }

        menuService.save(menu);
        ra.addFlashAttribute("successMsg", "메뉴 '" + menuName + "'이(가) 추가되었습니다.");
        return "redirect:/system/menu";
    }

    // ── 활성화 토글 ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        log.info("[SystemMenuController] 메뉴 토글 - menuId: {}, by: {}",
                id, auth != null ? auth.getName() : "unknown");
        try {
            menuService.toggleEnabled(id);
            ra.addFlashAttribute("successMsg", "메뉴 상태가 변경되었습니다.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/system/menu";
    }

    // ── 삭제 ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        log.info("[SystemMenuController] 메뉴 삭제 요청 - menuId: {}, by: {}",
                id, auth != null ? auth.getName() : "unknown");
        try {
            menuService.delete(id);
            ra.addFlashAttribute("successMsg", "메뉴가 삭제되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("[SystemMenuController] 메뉴 삭제 실패 - {}", e.getMessage());
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/system/menu";
    }

    // ── 역할 수정 ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/update")
    public String updateMenu(
            @PathVariable Long id,
            @RequestParam String menuName,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String url,
            Authentication auth,
            RedirectAttributes ra) {

        log.info("[SystemMenuController] 메뉴 수정 - menuId: {}, menuName: {}, by: {}",
                id, menuName, auth != null ? auth.getName() : "unknown");
        try {
            menuService.updateMenu(id, menuName, icon, url);
            ra.addFlashAttribute("successMsg", "메뉴가 수정되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/system/menu";
    }

    @PostMapping("/{id}/roles")
    public String updateRoles(
            @PathVariable Long id,
            @RequestParam(required = false) List<String> roles,
            Authentication auth,
            RedirectAttributes ra) {

        Set<String> roleSet = roles != null
                ? roles.stream().filter(AVAILABLE_ROLES::contains).collect(Collectors.toSet())
                : new HashSet<>();

        log.info("[SystemMenuController] 메뉴 역할 수정 - menuId: {}, roles: {}, by: {}",
                id, roleSet, auth != null ? auth.getName() : "unknown");
        try {
            menuService.updateRoles(id, roleSet);
            ra.addFlashAttribute("successMsg", "메뉴 역할이 수정되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/system/menu";
    }
}
