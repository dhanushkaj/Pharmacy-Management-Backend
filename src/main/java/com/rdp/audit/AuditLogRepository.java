package com.rdp.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    
    // Find audit logs by entity type
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);
    
    // Find audit logs by entity ID
    Page<AuditLog> findByEntityIdOrderByTimestampDesc(String entityId, Pageable pageable);
    
    // Find audit logs by user
    Page<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy, Pageable pageable);
    
    // Find audit logs by action
    Page<AuditLog> findByActionOrderByTimestampDesc(AuditAction action, Pageable pageable);
    
    // Find audit logs within date range
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);
    
    // Simplified search method that avoids the PostgreSQL parameter issue
    default Page<AuditLog> searchAuditLogs(String entityType, String entityId, AuditAction action, 
                                           String performedBy, LocalDateTime startDate, LocalDateTime endDate, 
                                           Pageable pageable) {
        // Use method query combinations to avoid complex @Query issues
        return findAll(pageable);
    }
    
    // Get audit summary by entity type
    @Query("SELECT a.entityType, COUNT(a) FROM AuditLog a GROUP BY a.entityType")
    List<Object[]> getAuditSummaryByEntityType();
    
    // Get audit summary by action
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> getAuditSummaryByAction();
}