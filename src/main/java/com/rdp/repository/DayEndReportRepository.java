package com.rdp.repository;

import com.rdp.model.DayEndReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayEndReportRepository extends JpaRepository<DayEndReport, Long> {
    DayEndReport findByDate(String date);
}
