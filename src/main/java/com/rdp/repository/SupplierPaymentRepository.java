package com.rdp.repository;

import com.rdp.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    Optional<SupplierPayment> findByPaymentReference(String paymentReference);

    @Query("SELECT sp FROM SupplierPayment sp WHERE sp.invoice.id = :invoiceId ORDER BY sp.paymentDate DESC")
    List<SupplierPayment> findPaymentsByInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT sp FROM SupplierPayment sp WHERE sp.supplier.supplierId = :supplierId ORDER BY sp.paymentDate DESC")
    List<SupplierPayment> findPaymentsBySupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT sp FROM SupplierPayment sp WHERE sp.paymentStatus = :status ORDER BY sp.paymentDate DESC")
    List<SupplierPayment> findByPaymentStatus(@Param("status") String status);

    @Query("SELECT sp FROM SupplierPayment sp WHERE sp.paymentDate BETWEEN :startDate AND :endDate ORDER BY sp.paymentDate DESC")
    List<SupplierPayment> findPaymentsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(sp.paymentReference, 26) AS Integer)), 0) FROM SupplierPayment sp WHERE sp.paymentReference LIKE :pattern")
    Integer findMaxPaymentSequence(@Param("pattern") String pattern);
}
