package com.idstory.client.repository;

import com.idstory.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 클라이언트 JPA 리포지토리
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, String> {

    List<Client> findByDeletedAtIsNullOrderBySortOrderAsc();

    boolean existsByClientCode(String clientCode);
}
