package com.rdp.controller;

import com.rdp.dto.DayEndReportRequest;
import com.rdp.model.DayEndReport;
import com.rdp.service.DayEndReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/day-end-report")
public class DayEndReportController {

    @Autowired
    private DayEndReportService dayEndReportService;

    @PostMapping
    public ResponseEntity<DayEndReport> submitDayEndReport(@RequestBody DayEndReportRequest request) {
        DayEndReport report = dayEndReportService.submitDayEndReport(request);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/details")
    public ResponseEntity<DayEndReport> getDayEndReportDetails(@RequestParam String date) {
        DayEndReport report = dayEndReportService.getDayEndReportDetails(date);
        return ResponseEntity.ok(report);
    }

    // Removed obsolete payment-summary endpoint
}
