package com.idstory.admin.repository;

import com.idstory.admin.entity.SysAdmin;
import com.idstory.user.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 관리자 추가 정보 JPA 리포지토리
 */
@Repository
public interface SysAdminRepository extends JpaRepository<SysAdmin, String> {

    /** 연결된 SysUser로 조회 */
    Optional<SysAdmin> findByUser(SysUser user);

    /** SysUser 기준 존재 여부 */
    boolean existsByUser(SysUser user);

    /**
     * 키워드 필터 + 페이징 조회 (SysUser JOIN FETCH)
     *
     * @param keyword 아이디/이름 키워드 (null=전체)
     */
    @Query(value = """
            SELECT a FROM SysAdmin a JOIN FETCH a.user u
            WHERE u.deletedAt IS NULL
              AND (:keyword IS NULL OR u.userId LIKE %:keyword% OR u.name LIKE %:keyword%)
            ORDER BY a.grantedAt DESC
            """,
           countQuery = """
            SELECT COUNT(a) FROM SysAdmin a JOIN a.user u
            WHERE u.deletedAt IS NULL
              AND (:keyword IS NULL OR u.userId LIKE %:keyword% OR u.name LIKE %:keyword%)
            """)
    Page<SysAdmin> findByFilter(@Param("keyword") String keyword, Pageable pageable);

    @Query("select a from SysAdmin a join fetch a.user where a.adminOid = :adminOid")
    Optional<SysAdmin> findAdminWithUser(@Param("adminOid") String adminOid);
}
