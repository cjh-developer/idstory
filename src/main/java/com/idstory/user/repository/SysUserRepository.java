package com.idstory.user.repository;

import com.idstory.user.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 시스템 사용자 JPA 리포지토리
 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, String> {

    /** 로그인 계정(user_id)으로 조회 */
    Optional<SysUser> findByUserId(String userId);

    /** 이메일로 조회 */
    Optional<SysUser> findByEmail(String email);

    /** 로그인 계정 중복 여부 확인 */
    boolean existsByUserId(String userId);

    /** 이메일 중복 여부 확인 */
    boolean existsByEmail(String email);

    /** 잠금 상태 계정 수 */
    long countByLockYn(String lockYn);

    /** 소프트 삭제 제외 전체 사용자 수 */
    long countByDeletedAtIsNull();

    /** 소프트 삭제 제외 + 사용여부별 사용자 수 */
    long countByDeletedAtIsNullAndUseYn(String useYn);

    /** 소프트 삭제 제외 + 역할별 사용자 수 */
    long countByDeletedAtIsNullAndRole(String role);

    /**
     * 특정 부서의 사용자 목록 (키워드 필터, 페이징, 소프트 삭제 제외)
     */
    @Query("""
            SELECT u FROM SysUser u
            WHERE u.deletedAt IS NULL
              AND u.deptCode = :deptCode
              AND (:keyword IS NULL OR u.userId LIKE %:keyword% OR u.name LIKE %:keyword%)
            ORDER BY u.name ASC
            """)
    Page<SysUser> findByDeptCode(@Param("deptCode") String deptCode,
                                 @Param("keyword")  String keyword,
                                 Pageable pageable);

    /**
     * 배정 모달용: 전체 활성 사용자 키워드 검색 (페이징)
     */
    @Query("""
            SELECT u FROM SysUser u
            WHERE u.deletedAt IS NULL
              AND (:keyword IS NULL OR u.userId LIKE %:keyword% OR u.name LIKE %:keyword%)
            ORDER BY u.name ASC
            """)
    Page<SysUser> findAssignableUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 관리자 등록 모달용: 일반 사용자(role=USER) 키워드 검색 (최대 20건)
     *
     * @param keyword 아이디/이름 키워드 (null=전체)
     */
    @Query("""
            SELECT u FROM SysUser u
            WHERE u.deletedAt IS NULL
              AND u.role = 'USER'
              AND (:keyword IS NULL OR u.userId LIKE %:keyword% OR u.name LIKE %:keyword%)
            ORDER BY u.name ASC
            """)
    List<SysUser> findNonAdminUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 다중 조건 필터 + 페이징 조회 (소프트 삭제 제외)
     *
     * @param keyword  아이디/이름 키워드 (null=전체)
     * @param deptCode 부서코드 (null=전체)
     * @param role     역할 (null=전체)
     * @param useYn    사용여부 (null=전체)
     * @param status   상태 (null=전체)
     * @param lockYn   잠금여부 (null=전체)
     */
    @Query("""
            SELECT u FROM SysUser u
            WHERE u.deletedAt IS NULL
              AND (:keyword  IS NULL OR u.userId LIKE %:keyword%  OR u.name LIKE %:keyword%)
              AND (:deptCode IS NULL OR u.deptCode = :deptCode)
              AND (:role     IS NULL OR u.role     = :role)
              AND (:useYn    IS NULL OR u.useYn    = :useYn)
              AND (:status   IS NULL OR u.status   = :status)
              AND (:lockYn   IS NULL OR u.lockYn   = :lockYn)
            ORDER BY u.createdAt DESC
            """)
    Page<SysUser> findByFilter(
            @Param("keyword")  String keyword,
            @Param("deptCode") String deptCode,
            @Param("role")     String role,
            @Param("useYn")    String useYn,
            @Param("status")   String status,
            @Param("lockYn")   String lockYn,
            Pageable pageable);
}
