package com.rdp.repository;

import com.rdp.model.InventoryAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryAuditRepository extends JpaRepository<InventoryAudit, Long> {
    
    @Query("FROM InventoryAudit ia WHERE ia.category.categoryId = :categoryId ORDER BY ia.exportedAt DESC")
    List<InventoryAudit> findByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("FROM InventoryAudit ia WHERE ia.status = :status ORDER BY ia.exportedAt DESC")
    List<InventoryAudit> findByStatus(@Param("status") String status);
    
    @Query("FROM InventoryAudit ia WHERE ia.category.categoryId = :categoryId AND ia.status = :status ORDER BY ia.exportedAt DESC")
    List<InventoryAudit> findByCategoryIdAndStatus(@Param("categoryId") Long categoryId, @Param("status") String status);
    
    @Query("FROM InventoryAudit ia ORDER BY ia.exportedAt DESC")
    Page<InventoryAudit> findAllAudits(Pageable pageable);
    
    @Query("FROM InventoryAudit ia WHERE ia.exportedAt BETWEEN :startDate AND :endDate ORDER BY ia.exportedAt DESC")
    List<InventoryAudit> findAuditsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("FROM InventoryAudit ia WHERE ia.category.categoryId = :categoryId ORDER BY ia.exportedAt DESC LIMIT 1")
    Optional<InventoryAudit> findLatestByCategory(@Param("categoryId") Long categoryId);
}
