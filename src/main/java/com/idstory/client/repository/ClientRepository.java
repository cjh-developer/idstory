package com.idstory.client.repository;

import com.idstory.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 클라이언트 JPA 리포지토리
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    List<Client> findByDeletedAtIsNullOrderBySortOrderAsc();

    boolean existsByClientCode(String clientCode);

    /**
     * 시스템별 배정 사용자 수 (role_user 기준, 대시보드용)
     * 반환: Object[]{ client_name, user_count }
     */
    @Query(value = "SELECT c.client_name, COALESCE(COUNT(DISTINCT ru.user_oid),0) AS user_count " +
                   "FROM ids_iam_client c " +
                   "LEFT JOIN ids_iam_permission p ON p.client_oid=c.client_oid AND p.deleted_at IS NULL " +
                   "LEFT JOIN ids_iam_perm_role pr ON pr.perm_oid=p.perm_oid " +
                   "LEFT JOIN ids_iam_role_user ru ON ru.role_oid=pr.role_oid " +
                   "WHERE c.deleted_at IS NULL AND c.parent_oid IS NULL " +
                   "GROUP BY c.client_oid, c.client_name ORDER BY user_count DESC",
           nativeQuery = true)
    List<Object[]> findClientUserCounts();
}
