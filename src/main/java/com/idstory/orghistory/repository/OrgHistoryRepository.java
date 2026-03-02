package com.idstory.orghistory.repository;

import com.idstory.orghistory.entity.OrgHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 조직 이력 JPA 리포지토리
 */
@Repository
public interface OrgHistoryRepository extends JpaRepository<OrgHistory, String> {
}
