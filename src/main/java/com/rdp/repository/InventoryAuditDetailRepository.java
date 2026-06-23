package com.rdp.repository;

import com.rdp.model.InventoryAuditDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryAuditDetailRepository extends JpaRepository<InventoryAuditDetail, Long> {
    
    @Query("FROM InventoryAuditDetail iad WHERE iad.audit.auditId = :auditId ORDER BY iad.product.name ASC")
    List<InventoryAuditDetail> findByAuditId(@Param("auditId") Long auditId);
    
    @Query("FROM InventoryAuditDetail iad WHERE iad.audit.auditId = :auditId AND iad.variance != 0 ORDER BY iad.variance DESC")
    List<InventoryAuditDetail> findVariancesByAuditId(@Param("auditId") Long auditId);
    
    @Query("FROM InventoryAuditDetail iad WHERE iad.audit.auditId = :auditId AND iad.product.productId = :productId")
    List<InventoryAuditDetail> findByAuditIdAndProductId(@Param("auditId") Long auditId, @Param("productId") Long productId);
}
