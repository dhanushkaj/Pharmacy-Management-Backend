package com.rdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdp.model.SupplierReturnItem;

public interface SupplierReturnItemRepository extends JpaRepository<SupplierReturnItem, Long> {
    /**
     * Find all items for a specific supplier return
     */
    @Query("select sri from SupplierReturnItem sri where sri.supplierReturn.returnId = :returnId")
    List<SupplierReturnItem> findBySupplierReturnId(@Param("returnId") Long returnId);

    /**
     * Delete all items for a specific supplier return
     */
    @Query("delete from SupplierReturnItem sri where sri.supplierReturn.returnId = :returnId")
    void deleteBySupplierReturnId(@Param("returnId") Long returnId);
}
