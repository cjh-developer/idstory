package com.idstory.depthead.repository;

import com.idstory.depthead.entity.DeptHead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 부서장 JPA 리포지토리
 */
@Repository
public interface DeptHeadRepository extends JpaRepository<DeptHead, String> {

    Optional<DeptHead> findByDeptOid(String deptOid);

    boolean existsByDeptOid(String deptOid);

    void deleteByDeptOid(String deptOid);
}
