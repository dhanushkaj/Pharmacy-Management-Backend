package com.rdp.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdp.model.SupplierReturn;

public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, Long> {
    /**
     * Find all supplier returns paginated
     */
    Page<SupplierReturn> findAll(Pageable pageable);

    /**
     * Find supplier returns by supplier ID
     */
    @Query("select sr from SupplierReturn sr where sr.supplier.supplierId = :supplierId order by sr.returnDate desc")
    List<SupplierReturn> findBySupplier(@Param("supplierId") Long supplierId);

    /**
     * Find supplier returns paginated by supplier ID
     */
    @Query("select sr from SupplierReturn sr where sr.supplier.supplierId = :supplierId order by sr.returnDate desc")
    Page<SupplierReturn> findBySupplierPaginated(@Param("supplierId") Long supplierId, Pageable pageable);

    /**
     * Find supplier returns by date range
     */
    @Query("select sr from SupplierReturn sr where sr.returnDate between :startDate and :endDate order by sr.returnDate desc")
    List<SupplierReturn> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Check if return number already exists
     */
    boolean existsByReturnNumber(String returnNumber);
}
