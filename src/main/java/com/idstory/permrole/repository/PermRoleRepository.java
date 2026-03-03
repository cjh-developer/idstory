package com.idstory.permrole.repository;

import com.idstory.permrole.entity.PermRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 권한-역할 매핑 JPA 리포지토리
 */
@Repository
public interface PermRoleRepository extends JpaRepository<PermRole, String> {

    List<PermRole> findByPermOid(String permOid);

    Optional<PermRole> findByPermOidAndRoleOid(String permOid, String roleOid);

    void deleteByPermOidAndRoleOid(String permOid, String roleOid);
}
