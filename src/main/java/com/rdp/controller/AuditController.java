package com.rdp.controller;

import com.rdp.audit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findAll(pageable);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<AuditLog>> searchAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Pageable pageable) {
        
        // Parse date strings to LocalDateTime
        LocalDateTime parsedStartDate = null;
        LocalDateTime parsedEndDate = null;
        
        try {
            if (startDate != null && !startDate.trim().isEmpty()) {
                // Handle different date formats
                String cleanStartDate = startDate.replace("Z", "").replace("+00:00", "");
                if (cleanStartDate.length() == 10) {
                    // Date only format: YYYY-MM-DD -> add time
                    cleanStartDate = cleanStartDate + "T00:00:00";
                }
                parsedStartDate = LocalDateTime.parse(cleanStartDate);
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                // Handle different date formats  
                String cleanEndDate = endDate.replace("Z", "").replace("+00:00", "");
                if (cleanEndDate.length() == 10) {
                    // Date only format: YYYY-MM-DD -> add time
                    cleanEndDate = cleanEndDate + "T23:59:59";
                }
                parsedEndDate = LocalDateTime.parse(cleanEndDate);
            }
        } catch (Exception e) {
            // Log parsing error for debugging
            System.err.println("Date parsing error - startDate: " + startDate + ", endDate: " + endDate + ", error: " + e.getMessage());
            parsedStartDate = null;
            parsedEndDate = null;
        }
        
        Page<AuditLog> auditLogs = auditService.searchAuditLogs(
            entityType, entityId, action, performedBy, parsedStartDate, parsedEndDate, pageable
        );
        
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/entity/{entityType}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEntityType(
            @PathVariable String entityType, 
            Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType, pageable);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId, 
            Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByEntityIdOrderByTimestampDesc(entityId, pageable);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/user/{username}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable String username, 
            Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByPerformedByOrderByTimestampDesc(username, pageable);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/action/{action}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByAction(
            @PathVariable AuditAction action, 
            Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByActionOrderByTimestampDesc(action, pageable);
        return ResponseEntity.ok(auditLogs);
    }
    
    @GetMapping("/summary/entity-types")
    public ResponseEntity<Map<String, Long>> getAuditSummaryByEntityType() {
        List<Object[]> results = auditLogRepository.getAuditSummaryByEntityType();
        Map<String, Long> summary = results.stream()
            .collect(Collectors.toMap(
                result -> (String) result[0],
                result -> (Long) result[1]
            ));
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/summary/actions")
    public ResponseEntity<Map<String, Long>> getAuditSummaryByAction() {
        List<Object[]> results = auditLogRepository.getAuditSummaryByAction();
        Map<String, Long> summary = results.stream()
            .collect(Collectors.toMap(
                result -> result[0].toString(),
                result -> (Long) result[1]
            ));
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> getRecentAuditLogs(@RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        Page<AuditLog> auditLogs = auditLogRepository.findAll(pageable);
        return ResponseEntity.ok(auditLogs.getContent());
    }
    
    @GetMapping("/export")
    public ResponseEntity<String> exportAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            // Parse date strings to LocalDateTime (reuse the same logic as search)
            LocalDateTime parsedStartDate = null;
            LocalDateTime parsedEndDate = null;
            
            if (startDate != null && !startDate.trim().isEmpty()) {
                String cleanStartDate = startDate.replace("Z", "").replace("+00:00", "");
                if (cleanStartDate.length() == 10) {
                    cleanStartDate = cleanStartDate + "T00:00:00";
                }
                parsedStartDate = LocalDateTime.parse(cleanStartDate);
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                String cleanEndDate = endDate.replace("Z", "").replace("+00:00", "");
                if (cleanEndDate.length() == 10) {
                    cleanEndDate = cleanEndDate + "T23:59:59";
                }
                parsedEndDate = LocalDateTime.parse(cleanEndDate);
            }
            
            // Get all audit logs matching the filters (no pagination for export)
            List<AuditLog> auditLogs = auditService.exportAuditLogs(
                entityType, entityId, action, performedBy, parsedStartDate, parsedEndDate
            );
            
            // Generate CSV content
            String csvContent = generateCsvContent(auditLogs);
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "audit_logs_" + timestamp + ".csv";
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .body(csvContent);
                
        } catch (Exception e) {
            System.err.println("Error exporting audit logs: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error exporting audit logs: " + e.getMessage());
        }
    }
    
    private String generateCsvContent(List<AuditLog> auditLogs) {
        StringBuilder csvBuilder = new StringBuilder();
        
        // CSV Header - Focused on useful change information
        csvBuilder.append("ID,Entity Type,Entity ID,Action,Performed By,Timestamp,Old Values,New Values,Description\n");
        
        // CSV Rows - Focused on useful change information
        for (AuditLog log : auditLogs) {
            csvBuilder.append(escapeField(log.getId().toString()))
                     .append(",")
                     .append(escapeField(log.getEntityType()))
                     .append(",")
                     .append(escapeField(log.getEntityId()))
                     .append(",")
                     .append(escapeField(log.getAction().toString()))
                     .append(",")
                     .append(escapeField(log.getPerformedBy()))
                     .append(",")
                     .append(escapeField(log.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                     .append(",")
                     .append(escapeField(log.getOldValues() != null ? log.getOldValues() : ""))
                     .append(",")
                     .append(escapeField(log.getNewValues() != null ? log.getNewValues() : ""))
                     .append(",")
                     .append(escapeField(log.getOperationDescription() != null ? log.getOperationDescription() : ""))
                     .append("\n");
        }
        
        return csvBuilder.toString();
    }
    
    private String escapeField(String field) {
        if (field == null) return "";
        
        // Escape double quotes by doubling them
        String escaped = field.replace("\"", "\"\"");
        
        // Wrap in quotes if field contains comma, newline, or quotes
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            escaped = "\"" + escaped + "\"";
        }
        
        return escaped;
    }
}