package com.idstory.dept.repository;

import com.idstory.dept.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 부서 JPA 리포지토리
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {

    /** 삭제되지 않고 사용중인 부서 목록 (정렬 순서 기준) */
    List<Department> findByUseYnAndDeletedAtIsNullOrderBySortOrderAsc(String useYn);

    /** 삭제되지 않은 부서 전체 목록 (정렬 순서 기준) */
    List<Department> findByDeletedAtIsNullOrderBySortOrderAsc();

    /** 삭제 포함 전체 부서 목록 (정렬 순서 기준) */
    List<Department> findAllByOrderBySortOrderAsc();

    /** 부서코드로 부서 조회 */
    Optional<Department> findByDeptCode(String deptCode);

    /** 부서코드 존재 여부 확인 */
    boolean existsByDeptCode(String deptCode);

    /** 부서코드 중복 확인 (본인 제외) */
    boolean existsByDeptCodeAndDeptOidNot(String deptCode, String deptOid);
}
