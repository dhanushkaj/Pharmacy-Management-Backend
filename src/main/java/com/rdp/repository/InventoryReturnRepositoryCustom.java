package com.rdp.repository;

import com.rdp.model.InventoryReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryReturnRepositoryCustom extends JpaRepository<InventoryReturn, Long> {
    @Query("SELECT ir FROM InventoryReturn ir WHERE ir.returnType = com.rdp.model.InventoryReturn.ReturnType.FROM_CUSTOMER AND ir.returnDate BETWEEN :start AND :end")
    List<InventoryReturn> findCustomerReturnsForDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
