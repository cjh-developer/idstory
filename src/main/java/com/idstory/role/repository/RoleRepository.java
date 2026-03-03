package com.idstory.role.repository;

import com.idstory.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 역할 JPA 리포지토리
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    List<Role> findByDeletedAtIsNullOrderBySortOrderAsc();

    boolean existsByRoleCode(String roleCode);
}
