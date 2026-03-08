package com.idstory.sso.repository;

import com.idstory.sso.entity.SsoClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SsoClientRepository extends JpaRepository<SsoClient, String> {
    Optional<SsoClient> findByClientId(String clientId);
    Optional<SsoClient> findByClientOid(String clientOid);
    boolean existsByClientOid(String clientOid);
    boolean existsByClientId(String clientId);
    List<SsoClient> findAllByOrderByCreatedAtDesc();
}
