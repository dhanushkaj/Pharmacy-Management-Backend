package com.rdp.repository;

import com.rdp.model.RdpDayEndManualBill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RdpDayEndManualBillRepository extends JpaRepository<RdpDayEndManualBill, Long> {
    List<RdpDayEndManualBill> findByReportDate(String reportDate);
}
