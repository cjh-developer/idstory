package com.idstory.grade.repository;

import com.idstory.grade.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 직급 JPA 리포지토리
 */
@Repository
public interface GradeRepository extends JpaRepository<Grade, String> {

    boolean existsByGradeCode(String gradeCode);

    @Query("""
            SELECT g FROM Grade g
            WHERE (:includeDeleted = true OR g.deletedAt IS NULL)
              AND (:gradeCode IS NULL OR g.gradeCode LIKE %:gradeCode%)
              AND (:gradeName IS NULL OR g.gradeName LIKE %:gradeName%)
              AND (:useYn     IS NULL OR g.useYn     = :useYn)
            ORDER BY g.sortOrder ASC, g.gradeCode ASC
            """)
    Page<Grade> findByFilter(
            @Param("gradeCode")      String gradeCode,
            @Param("gradeName")      String gradeName,
            @Param("useYn")          String useYn,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);
}
