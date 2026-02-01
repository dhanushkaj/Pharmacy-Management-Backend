package com.rdp.repository;

import com.rdp.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

        List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    @Query("SELECT SUM(CASE WHEN sm.toBin = com.rdp.model.BinType.INVENTORY THEN sm.quantity WHEN sm.fromBin = com.rdp.model.BinType.INVENTORY THEN -sm.quantity ELSE 0 END) " +
            "FROM StockMovement sm WHERE sm.productId = :productId AND sm.price = :price")
    Integer getInventoryBalanceForProductAndPrice(@Param("productId") Long productId, @Param("price") java.math.BigDecimal price);

    @Query("SELECT SUM(CASE WHEN sm.toBin = com.rdp.model.BinType.INVENTORY THEN sm.quantity WHEN sm.fromBin = com.rdp.model.BinType.INVENTORY THEN -sm.quantity ELSE 0 END) " +
            "FROM StockMovement sm WHERE sm.productId = :productId AND sm.batchNo = :batchNo")
    Integer getInventoryBalanceForBatch(@Param("productId") Long productId, @Param("batchNo") String batchNo);

    @Query("SELECT SUM(CASE WHEN sm.toBin = com.rdp.model.BinType.INVENTORY THEN sm.quantity WHEN sm.fromBin = com.rdp.model.BinType.INVENTORY THEN -sm.quantity ELSE 0 END) " +
            "FROM StockMovement sm WHERE sm.productId = :productId")
    Integer getInventoryBalanceForProduct(@Param("productId") Long productId);

    List<StockMovement> findByProductIdAndBatchNoOrderByCreatedAtAsc(Long productId, String batchNo);
}
