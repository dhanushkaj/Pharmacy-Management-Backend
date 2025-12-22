package com.rdp.controller;

import com.rdp.dto.*;
import com.rdp.model.AlertConfig;
import com.rdp.model.AlertLog;
import com.rdp.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * Get alert summary for dashboard
     */
    @GetMapping("/summary")
    public ResponseEntity<AlertSummaryResponse> getAlertSummary() {
        return ResponseEntity.ok(alertService.getAlertSummary());
    }

    /**
     * Get paginated alerts with filters
     */
    @GetMapping
    public ResponseEntity<Page<AlertResponse>> getAlerts(
            @RequestParam(required = false, defaultValue = "ACTIVE") AlertLog.AlertStatus status,
            @RequestParam(required = false) AlertConfig.AlertSeverity severity,
            @RequestParam(required = false) AlertConfig.AlertType alertType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        
        Page<AlertResponse> alerts = alertService.getAlerts(
            status, severity, alertType, startDate, endDate, pageable);
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * Acknowledge a single alert
     */
    @PutMapping("/{alertLogId}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledgeAlert(@PathVariable Long alertLogId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertLogId));
    }

    /**
     * Acknowledge multiple alerts
     */
    @PutMapping("/acknowledge-multiple")
    public ResponseEntity<Void> acknowledgeMultipleAlerts(@RequestBody List<Long> alertIds) {
        alertService.acknowledgeMultipleAlerts(alertIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Manually trigger alert generation
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generateAlerts() {
        alertService.generateExpiryAlerts();
        return ResponseEntity.ok().build();
    }

    // ============= Alert Configuration Endpoints =============

    /**
     * Get all alert configurations
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AlertConfigResponse>> getAllConfigurations() {
        return ResponseEntity.ok(alertService.getAllConfigurations());
    }

    /**
     * Get a specific configuration
     */
    @GetMapping("/config/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlertConfigResponse> getConfiguration(@PathVariable Long configId) {
        return ResponseEntity.ok(alertService.getConfiguration(configId));
    }

    /**
     * Create new alert configuration
     */
    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlertConfigResponse> createConfiguration(@RequestBody AlertConfigRequest request) {
        return ResponseEntity.ok(alertService.createConfiguration(request));
    }

    /**
     * Update alert configuration
     */
    @PutMapping("/config/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlertConfigResponse> updateConfiguration(
            @PathVariable Long configId,
            @RequestBody AlertConfigRequest request) {
        return ResponseEntity.ok(alertService.updateConfiguration(configId, request));
    }

    /**
     * Delete alert configuration
     */
    @DeleteMapping("/config/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long configId) {
        alertService.deleteConfiguration(configId);
        return ResponseEntity.ok().build();
    }

    /**
     * Initialize default configurations
     */
    @PostMapping("/config/initialize-defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> initializeDefaults() {
        alertService.useDefaultConfigurations();
        return ResponseEntity.ok().build();
    }
}
