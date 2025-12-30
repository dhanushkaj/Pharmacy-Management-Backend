package com.rdp.repository;

import com.rdp.model.BillingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingItemRepository extends JpaRepository<BillingItem, Long> {

    List<BillingItem> findByBillingBillingId(Long billingId);
    
    List<BillingItem> findByProductProductId(Long productId);
}
