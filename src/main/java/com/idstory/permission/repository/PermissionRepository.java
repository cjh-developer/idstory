package com.idstory.permission.repository;

import com.idstory.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 권한 JPA 리포지토리
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    List<Permission> findByClientOidAndDeletedAtIsNullOrderBySortOrderAsc(String clientOid);

    boolean existsByPermCode(String permCode);
}
