package com.idstory.sso.repository;

import com.idstory.sso.entity.SsoKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SsoKeyPairRepository extends JpaRepository<SsoKeyPair, String> {
    Optional<SsoKeyPair> findFirstByActiveYnOrderByCreatedAtAsc(String activeYn);
}
