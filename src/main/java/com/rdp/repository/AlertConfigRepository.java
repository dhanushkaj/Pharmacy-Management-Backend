package com.rdp.repository;

import com.rdp.model.AlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, Long> {
    
    List<AlertConfig> findByEnabledTrueOrderByThresholdDaysAsc();
    
    Optional<AlertConfig> findByAlertTypeAndEnabled(AlertConfig.AlertType alertType, Boolean enabled);
    
    List<AlertConfig> findByAlertType(AlertConfig.AlertType alertType);
}
