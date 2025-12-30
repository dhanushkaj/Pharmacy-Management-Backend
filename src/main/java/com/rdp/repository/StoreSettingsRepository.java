package com.rdp.repository;

import com.rdp.model.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
    // Get the first (and only) settings record
    Optional<StoreSettings> findFirstByOrderBySettingsIdAsc();
}
