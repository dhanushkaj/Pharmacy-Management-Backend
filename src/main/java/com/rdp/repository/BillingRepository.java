        // Add method to fetch all billings for a date range (no pagination)
       
// (removed stray method declaration outside interface)
package com.rdp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdp.model.Billing;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {
    @Query("FROM Billing b WHERE b.billingDate BETWEEN :start AND :end")
    List<Billing> findByBillingDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("FROM Billing b WHERE b.billingNumber = :billingNumber")
    Optional<Billing> findByBillingNumber(@Param("billingNumber") String billingNumber);

    Page<Billing> findAllByOrderByBillingDateDesc(Pageable pageable);

    @Query("FROM Billing b WHERE b.customer.customerId = :customerId ORDER BY b.billingDate DESC")
    List<Billing> findByCustomerCustomerIdOrderByBillingDateDesc(@Param("customerId") Long customerId);

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
