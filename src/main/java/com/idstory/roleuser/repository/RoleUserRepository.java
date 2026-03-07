package com.idstory.roleuser.repository;

import com.idstory.roleuser.entity.RoleUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleUserRepository extends JpaRepository<RoleUser, String> {

    List<RoleUser> findByRoleOid(String roleOid);

    List<RoleUser> findByUserOid(String userOid);

    Optional<RoleUser> findByRoleOidAndUserOid(String roleOid, String userOid);

    boolean existsByRoleOidAndUserOid(String roleOid, String userOid);

    void deleteByRoleOidAndUserOid(String roleOid, String userOid);
}
