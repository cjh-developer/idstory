package com.idstory.userorgmap.repository;

import com.idstory.userorgmap.entity.UserOrgMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사용자-조직 매핑 JPA 리포지토리
 */
@Repository
public interface UserOrgMapRepository extends JpaRepository<UserOrgMap, String> {

    /** 사용자 OID로 조직 매핑 목록 조회 (주소속 먼저, 생성일 오름차순) */
    List<UserOrgMap> findByUserOidOrderByIsPrimaryDescCreatedAtAsc(String userOid);

    /** 사용자 OID + 주소속 여부로 조회 */
    List<UserOrgMap> findByUserOidAndIsPrimary(String userOid, String isPrimary);

    /** 특정 부서 OID + 겸직(is_primary='N') 매핑 조회 */
    List<UserOrgMap> findByDeptOidAndIsPrimary(String deptOid, String isPrimary);

    /** 겸직 중복 체크: 동일 사용자·부서·isPrimary 조합 존재 여부 */
    boolean existsByUserOidAndDeptOidAndIsPrimary(String userOid, String deptOid, String isPrimary);
}
