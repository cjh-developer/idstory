package com.idstory.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 시스템 메뉴 엔티티
 *
 * <p>트리 구조: parent_id NULL = 최상위, parent_id 설정 = 하위 메뉴</p>
 * <p>sort_order 기준으로 정렬하여 사이드바에 렌더링됩니다.</p>
 */
@Entity
@Table(name = "ids_iam_menu")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long menuId;

    /** 상위 메뉴 ID (NULL = 최상위) */
    @Column(name = "parent_id")
    private Long parentId;

    /** 메뉴명 (화면 표시용) */
    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    /** Font Awesome 아이콘 클래스 (예: "fas fa-users") */
    @Column(name = "icon", length = 100)
    private String icon;

    /** 링크 URL (NULL 또는 '#' = 폴더형 부모 메뉴) */
    @Column(name = "url", length = 255)
    private String url;

    /** 정렬 순서 (오름차순) */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 활성화 여부 (false = 사이드바에서 숨김) */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /**
     * 잠금 여부 — true인 메뉴는 관리 페이지에서 토글/삭제 불가
     * (예: 대시보드는 항상 잠금)
     */
    @Column(name = "locked", nullable = false)
    private boolean locked;

    /**
     * 접근 허용 권한 목록 (menu_roles 테이블 매핑)
     * <ul>
     *   <li>비어있으면 모든 인증 사용자에게 표시</li>
     *   <li>값이 있으면 해당 권한 보유자에게만 표시</li>
     * </ul>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ids_iam_menu_role",
            joinColumns = @JoinColumn(name = "menu_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── 트리 구성용 transient 필드 (DB에 저장되지 않음) ────────────────────
    @Transient
    @Builder.Default
    private List<Menu> children = new ArrayList<>();

    /** null 안전 roles 반환 */
    public Set<String> getRoles() {
        return roles != null ? roles : new HashSet<>();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 폴더형 여부 (URL이 없거나 '#'인 경우) */
    public boolean isFolder() {
        return url == null || url.isBlank() || "#".equals(url.trim());
    }
}
