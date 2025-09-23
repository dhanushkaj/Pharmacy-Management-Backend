// src/main/java/com/rdp/repository/PurchaseOrderItemRepository.java
package com.rdp.repository;

import com.rdp.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    @Query("select i from PurchaseOrderItem i where i.id = :itemId and i.purchaseOrder.id = :orderId")
    Optional<PurchaseOrderItem> findOneInOrder(@Param("itemId") Long itemId, @Param("orderId") Long orderId);

    @Modifying
    @Query("delete from PurchaseOrderItem i where i.id = :itemId and i.purchaseOrder.id = :orderId")
    void deleteOneInOrder(@Param("itemId") Long itemId, @Param("orderId") Long orderId);
}