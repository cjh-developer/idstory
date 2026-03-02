package com.idstory.position.repository;

import com.idstory.position.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 직위 JPA 리포지토리
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, String> {

    boolean existsByPositionCode(String positionCode);

    boolean existsByPositionCodeAndPositionOidNot(String positionCode, String positionOid);

    /**
     * 다중 조건 필터 + 페이징 조회
     *
     * @param positionCode   직위코드 키워드 (null=전체)
     * @param positionName   직위명 키워드 (null=전체)
     * @param useYn          사용여부 (null=전체)
     * @param includeDeleted 삭제된 항목 포함 여부
     */
    @Query("""
            SELECT p FROM Position p
            WHERE (:includeDeleted = true OR p.deletedAt IS NULL)
              AND (:positionCode IS NULL OR p.positionCode LIKE %:positionCode%)
              AND (:positionName IS NULL OR p.positionName LIKE %:positionName%)
              AND (:useYn IS NULL OR p.useYn = :useYn)
            ORDER BY p.sortOrder ASC, p.positionCode ASC
            """)
    Page<Position> findByFilter(
            @Param("positionCode")   String positionCode,
            @Param("positionName")   String positionName,
            @Param("useYn")          String useYn,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);
}
