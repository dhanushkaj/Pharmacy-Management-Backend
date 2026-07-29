package com.rdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdp.model.InventoryCountLine;

@Repository
public interface InventoryCountLineRepository extends JpaRepository<InventoryCountLine, Long> {
    
    @Query("SELECT l FROM InventoryCountLine l WHERE l.session.id = :sessionId ORDER BY l.productCode")
    List<InventoryCountLine> findBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT l FROM InventoryCountLine l WHERE l.session.id = :sessionId AND l.counted = false")
    List<InventoryCountLine> findUncountedBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT l FROM InventoryCountLine l WHERE l.session.id = :sessionId AND l.variance != 0")
    List<InventoryCountLine> findLinesBySessionIdWithVariance(@Param("sessionId") Long sessionId);
}
