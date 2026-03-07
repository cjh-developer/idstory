package com.idstory.roleuser.repository;

import com.idstory.roleuser.entity.RoleSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleSubjectRepository extends JpaRepository<RoleSubject, String> {

    List<RoleSubject> findByRoleOidAndSubjectType(String roleOid, String subjectType);

    List<RoleSubject> findByRoleOid(String roleOid);

    Optional<RoleSubject> findByRoleOidAndSubjectTypeAndSubjectOid(
            String roleOid, String subjectType, String subjectOid);

    boolean existsByRoleOidAndSubjectTypeAndSubjectOid(
            String roleOid, String subjectType, String subjectOid);
}
