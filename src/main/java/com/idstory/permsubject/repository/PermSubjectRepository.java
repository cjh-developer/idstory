package com.idstory.permsubject.repository;

import com.idstory.permsubject.entity.PermSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 권한 대상 JPA 리포지토리
 */
@Repository
public interface PermSubjectRepository extends JpaRepository<PermSubject, String> {

    /** 권한 OID로 전체 대상 조회 */
    List<PermSubject> findByPermOid(String permOid);

    /** 권한 OID + 대상 유형으로 조회 */
    List<PermSubject> findByPermOidAndSubjectType(String permOid, String subjectType);

    /** 중복 체크용 단건 조회 */
    Optional<PermSubject> findByPermOidAndSubjectTypeAndSubjectOid(
            String permOid, String subjectType, String subjectOid);
}
