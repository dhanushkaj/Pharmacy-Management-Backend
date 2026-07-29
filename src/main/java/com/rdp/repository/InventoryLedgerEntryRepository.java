package com.rdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdp.model.InventoryLedgerEntry;
import com.rdp.model.MovementReasonType;

@Repository
public interface InventoryLedgerEntryRepository extends JpaRepository<InventoryLedgerEntry, Long> {
    
    @Query("SELECT l FROM InventoryLedgerEntry l WHERE l.product.productId = :productId ORDER BY l.createdAt DESC")
    List<InventoryLedgerEntry> findByProductId(@Param("productId") Long productId);
    
    @Query("SELECT l FROM InventoryLedgerEntry l WHERE l.referenceSessionId = :sessionId ORDER BY l.createdAt DESC")
    List<InventoryLedgerEntry> findByReferenceSessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT l FROM InventoryLedgerEntry l WHERE l.reasonType = :reasonType ORDER BY l.createdAt DESC")
    List<InventoryLedgerEntry> findByReasonType(@Param("reasonType") MovementReasonType reasonType);
}
