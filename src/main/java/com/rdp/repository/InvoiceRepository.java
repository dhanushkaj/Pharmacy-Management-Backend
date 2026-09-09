package com.rdp.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdp.model.Invoice;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.grn.id = :grnId")
    Optional<Invoice> findByGrnId(@Param("grnId") Long grnId);

    @Query("SELECT i FROM Invoice i WHERE i.supplier.supplierId = :supplierId ORDER BY i.invoiceDate DESC")
    List<Invoice> findBySupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT i FROM Invoice i WHERE i.paymentStatus = :status ORDER BY i.invoiceDate DESC")
    List<Invoice> findByPaymentStatus(@Param("status") String status);

    @Query("SELECT i FROM Invoice i WHERE i.supplier.supplierId = :supplierId AND i.paymentStatus != 'PAID' ORDER BY i.paymentDueDate ASC")
    List<Invoice> findUnpaidInvoicesBySupplier(@Param("supplierId") Long supplierId);

    @Query("SELECT i FROM Invoice i WHERE i.paymentStatus != 'PAID' ORDER BY i.invoiceDate DESC")
    List<Invoice> findAllUnpaidInvoices();

    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate ORDER BY i.invoiceDate DESC")
    List<Invoice> findInvoicesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
