package com.idstory.menu.repository;

import com.idstory.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 메뉴 레포지토리
 * JOIN FETCH 로 menu_roles 를 한 번에 조회하여 N+1 쿼리를 방지합니다.
 */
public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 활성화 메뉴 전체 + roles 한 번에 조회 (사이드바용)
     */
    @Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.roles " +
           "WHERE m.enabled = true ORDER BY m.sortOrder ASC")
    List<Menu> findEnabledWithRoles();

    /**
     * 전체 메뉴 + roles 한 번에 조회 (관리 페이지용)
     */
    @Query("SELECT DISTINCT m FROM Menu m LEFT JOIN FETCH m.roles " +
           "ORDER BY m.sortOrder ASC")
    List<Menu> findAllWithRoles();

    /** 최상위 메뉴만 — 하위 메뉴 추가 시 부모 선택 드롭다운용 */
    List<Menu> findByParentIdIsNullOrderBySortOrderAsc();

    /** 특정 부모의 하위 메뉴 존재 여부 */
    boolean existsByParentId(Long parentId);
}
