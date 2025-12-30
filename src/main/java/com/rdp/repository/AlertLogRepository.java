package com.rdp.repository;

import com.rdp.model.AlertLog;
import com.rdp.model.AlertConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    
    Page<AlertLog> findByStatusOrderByCreatedAtDesc(AlertLog.AlertStatus status, Pageable pageable);
    
    Page<AlertLog> findByStatusAndSeverityOrderByCreatedAtDesc(
        AlertLog.AlertStatus status, 
        AlertConfig.AlertSeverity severity, 
        Pageable pageable
    );
    
    List<AlertLog> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, AlertLog.AlertStatus status);
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = :status AND a.severity = :severity")
    Long countByStatusAndSeverity(
        @Param("status") AlertLog.AlertStatus status, 
        @Param("severity") AlertConfig.AlertSeverity severity
    );
    
    @Query("SELECT COUNT(a) FROM AlertLog a WHERE a.status = :status")
    Long countByStatus(@Param("status") AlertLog.AlertStatus status);
    
    @Query("SELECT a FROM AlertLog a WHERE a.status = :status " +
           "AND (:severity IS NULL OR a.severity = :severity) " +
           "AND (:alertType IS NULL OR a.alertType = :alertType) " +
           "AND (:startDate IS NULL OR a.expiryDate >= :startDate) " +
           "AND (:endDate IS NULL OR a.expiryDate <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AlertLog> findWithFilters(
        @Param("status") AlertLog.AlertStatus status,
        @Param("severity") AlertConfig.AlertSeverity severity,
        @Param("alertType") AlertConfig.AlertType alertType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
    
    // Deactivate old alerts for products that are no longer expiring soon
    @Query("UPDATE AlertLog a SET a.status = 'RESOLVED' " +
           "WHERE a.productId = :productId AND a.status = 'ACTIVE' " +
           "AND a.alertType IN ('EXPIRY_WARNING', 'EXPIRY_CRITICAL')")
    void resolveProductExpiryAlerts(@Param("productId") Long productId);
}
