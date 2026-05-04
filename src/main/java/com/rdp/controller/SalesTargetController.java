package com.rdp.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.model.SalesTarget;
import com.rdp.service.SalesTargetService;

@RestController
@RequestMapping("/api/sales-targets")
public class SalesTargetController {

    @Autowired
    private SalesTargetService salesTargetService;

    /**
     * Get all targets for a specific month
     */
    @GetMapping("/{year}/{month}")
    @PreAuthorize("hasAnyAuthority('admin', 'manager')")
    public ResponseEntity<List<SalesTarget>> getTargetsForMonth(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        List<SalesTarget> targets = salesTargetService.getTargetsForMonth(year, month);
        return ResponseEntity.ok(targets);
    }

    /**
     * Save or update targets for a month (bulk save)
     */
    @PostMapping("/{year}/{month}")
    @PreAuthorize("hasAnyAuthority('admin', 'manager')")
    public ResponseEntity<Map<String, String>> saveTargetsForMonth(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestBody List<SalesTarget> targets) {
        salesTargetService.saveTargetsForMonth(year, month, targets);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Targets saved successfully"));
    }

    /**
     * Save or update a single day's target
     */
    @PutMapping("/{year}/{month}/{day}")
    @PreAuthorize("hasAnyAuthority('admin', 'manager')")
    public ResponseEntity<SalesTarget> saveOrUpdateTarget(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Integer day,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal targetAmount = body.get("targetAmount");
        SalesTarget saved = salesTargetService.saveOrUpdateTarget(year, month, day, targetAmount);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get dashboard data for today's sales vs target and accumulated sales vs target
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('admin', 'manager', 'pharmacist')")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = salesTargetService.getDashboardData();
        return ResponseEntity.ok(data);
    }

    /**
     * Get monthly breakdown of sales vs targets (for chart display)
     */
    @GetMapping("/breakdown/{year}/{month}")
    @PreAuthorize("hasAnyAuthority('admin', 'manager', 'pharmacist')")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyBreakdown(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        List<Map<String, Object>> breakdown = salesTargetService.getMonthlyBreakdown(year, month);
        return ResponseEntity.ok(breakdown);
    }
}
