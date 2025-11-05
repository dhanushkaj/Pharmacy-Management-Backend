package com.rdp.repository;

import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByProduct(Product product);
    Optional<InventoryItem> findByProductAndPrice(Product product, BigDecimal price);
    List<InventoryItem> findByProductProductIdOrderByCreatedAtDesc(Long productId);
    Optional<InventoryItem> findByProductProductIdAndPrice(Long productId, BigDecimal price);
    boolean existsByProductProductIdAndCostPrice(Long productId, BigDecimal costPrice);

    Optional<InventoryItem> findByProductAndCostPriceAndPrice(Product product, BigDecimal costPrice, BigDecimal price);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.product.productId = :productId and i.price = :price")
    Optional<InventoryItem> findByProductAndPriceForUpdate(@Param("productId") Long productId, @Param("price") BigDecimal price);
}