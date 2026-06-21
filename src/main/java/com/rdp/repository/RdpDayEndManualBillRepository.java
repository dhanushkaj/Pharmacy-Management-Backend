package com.rdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdp.model.RdpDayEndManualBill;

public interface RdpDayEndManualBillRepository extends JpaRepository<RdpDayEndManualBill, Long> {
    @Query("FROM RdpDayEndManualBill rb WHERE rb.reportDate = :reportDate")
    List<RdpDayEndManualBill> findByReportDate(@Param("reportDate") String reportDate);
}
