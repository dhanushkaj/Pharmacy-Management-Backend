package com.rdp.repository;

import com.rdp.model.InventoryReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryReturnRepository extends JpaRepository<InventoryReturn, Long> {

    Page<InventoryReturn> findAllByOrderByReturnDateDesc(Pageable pageable);

    List<InventoryReturn> findByProductProductIdOrderByReturnDateDesc(Long productId);

    @Query("SELECT ir FROM InventoryReturn ir WHERE ir.returnType = :returnType ORDER BY ir.returnDate DESC")
    Page<InventoryReturn> findByReturnType(@Param("returnType") InventoryReturn.ReturnType returnType, Pageable pageable);

    @Query("SELECT ir FROM InventoryReturn ir WHERE ir.returnDate BETWEEN :startDate AND :endDate ORDER BY ir.returnDate DESC")
    Page<InventoryReturn> findByReturnDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
