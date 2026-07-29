package com.rdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdp.model.CountSessionStatus;
import com.rdp.model.InventoryCountSession;

@Repository
public interface InventoryCountSessionRepository extends JpaRepository<InventoryCountSession, Long> {
    
    @Query("SELECT s FROM InventoryCountSession s WHERE s.category.id = :categoryId ORDER BY s.versionNumber DESC")
    List<InventoryCountSession> findByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT s FROM InventoryCountSession s WHERE s.category.id = :categoryId AND s.versionNumber = :versionNumber")
    Optional<InventoryCountSession> findByCategoryIdAndVersionNumber(
            @Param("categoryId") Long categoryId,
            @Param("versionNumber") Integer versionNumber);
    
    @Query("SELECT s FROM InventoryCountSession s WHERE s.category.id = :categoryId AND s.status = :status ORDER BY s.createdAt DESC")
    Optional<InventoryCountSession> findDraftByCategoryId(
            @Param("categoryId") Long categoryId,
            @Param("status") CountSessionStatus status);
    
    @Query("SELECT s FROM InventoryCountSession s WHERE s.status = :status ORDER BY s.createdAt DESC")
    Page<InventoryCountSession> findByStatus(@Param("status") CountSessionStatus status, Pageable pageable);
    
    @Query("SELECT s FROM InventoryCountSession s WHERE s.category.id = :categoryId AND s.status = :status")
    List<InventoryCountSession> findByCategoryIdAndStatus(
            @Param("categoryId") Long categoryId,
            @Param("status") CountSessionStatus status);
}
