        // Add method to fetch all billings for a date range (no pagination)
       
// (removed stray method declaration outside interface)
package com.rdp.repository;

import com.rdp.model.Billing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {
        // Add method to fetch all billings for a date range (no pagination)
    List<Billing> findByBillingDateBetween(LocalDateTime start, LocalDateTime end);

    Optional<Billing> findByBillingNumber(String billingNumber);

    Page<Billing> findAllByOrderByBillingDateDesc(Pageable pageable);

    List<Billing> findByCustomerCustomerIdOrderByBillingDateDesc(Long customerId);

    @Query("SELECT b FROM Billing b WHERE b.billingDate BETWEEN :startDate AND :endDate ORDER BY b.billingDate DESC")
    Page<Billing> findByBillingDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT b FROM Billing b JOIN b.items i WHERE i.product.productId = :productId ORDER BY b.billingDate DESC")
    Page<Billing> findByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT b FROM Billing b WHERE b.customer.customerId = :customerId AND b.billingDate BETWEEN :startDate AND :endDate ORDER BY b.billingDate DESC")
    Page<Billing> findByCustomerAndDateRange(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    
}
