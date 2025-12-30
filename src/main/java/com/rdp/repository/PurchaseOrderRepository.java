package com.rdp.repository;

import com.rdp.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    boolean existsByOrderCodeIgnoreCase(String orderCode);

    Optional<PurchaseOrder> findTopByOrderCodeStartingWithOrderByOrderCodeDesc(String prefix);
}