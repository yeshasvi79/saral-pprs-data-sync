package com.pprs.sync.repository;

import com.pprs.sync.model.Security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityRepository extends JpaRepository<Security, Long> {
    // No custom upsert here — handled by JdbcTemplate in SyncService
    Optional<Security> findByIsinAndExchange(String isin, String exchange);
}
