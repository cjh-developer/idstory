package com.idstory.comprole.repository;

import com.idstory.comprole.entity.CompRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 직책 JPA 리포지토리
 */
@Repository
public interface CompRoleRepository extends JpaRepository<CompRole, String> {

    boolean existsByCompRoleCode(String compRoleCode);

    @Query("""
            SELECT r FROM CompRole r
            WHERE (:includeDeleted = true OR r.deletedAt IS NULL)
              AND (:compRoleCode IS NULL OR r.compRoleCode LIKE %:compRoleCode%)
              AND (:compRoleName IS NULL OR r.compRoleName LIKE %:compRoleName%)
              AND (:useYn        IS NULL OR r.useYn        = :useYn)
            ORDER BY r.sortOrder ASC, r.compRoleCode ASC
            """)
    Page<CompRole> findByFilter(
            @Param("compRoleCode")   String compRoleCode,
            @Param("compRoleName")   String compRoleName,
            @Param("useYn")          String useYn,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);
}
