package com.rdp.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdp.model.InventoryItem;
import com.rdp.model.Product;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    @Query("FROM InventoryItem ii WHERE ii.product = :product")
    List<InventoryItem> findByProduct(@Param("product") Product product);
    
    @Query("FROM InventoryItem ii WHERE ii.product = :product AND ii.price = :price")
    Optional<InventoryItem> findByProductAndPrice(@Param("product") Product product, @Param("price") BigDecimal price);
    
    @Query("FROM InventoryItem ii WHERE ii.product.productId = :productId ORDER BY ii.createdAt DESC")
    List<InventoryItem> findByProductProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

    @Query("FROM InventoryItem ii WHERE ii.product.productId = :productId ORDER BY ii.createdAt ASC")
    List<InventoryItem> findByProductProductIdOrderByCreatedAtAsc(@Param("productId") Long productId);
    
    @Query("FROM InventoryItem ii WHERE ii.product.productId = :productId AND ii.price = :price")
    Optional<InventoryItem> findByProductProductIdAndPrice(@Param("productId") Long productId, @Param("price") BigDecimal price);
    
    @Query("SELECT CASE WHEN COUNT(ii) > 0 THEN true ELSE false END FROM InventoryItem ii WHERE ii.product.productId = :productId AND ii.costPrice = :costPrice")
    boolean existsByProductProductIdAndCostPrice(@Param("productId") Long productId, @Param("costPrice") BigDecimal costPrice);

    @Query("FROM InventoryItem ii WHERE ii.product = :product AND ii.costPrice = :costPrice AND ii.price = :price")
    Optional<InventoryItem> findByProductAndCostPriceAndPrice(@Param("product") Product product, @Param("costPrice") BigDecimal costPrice, @Param("price") BigDecimal price);

    @Query(value = "SELECT * FROM rdp_inventory_items WHERE product_id = :productId AND price = :price FOR UPDATE", nativeQuery = true)
    Optional<InventoryItem> findByProductIdAndPriceForUpdateNative(@Param("productId") Long productId, @Param("price") BigDecimal price);
}