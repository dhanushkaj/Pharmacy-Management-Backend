package com.rdp.controller;

import com.rdp.dto.InventoryAuditDto;
import com.rdp.dto.InventoryAuditDetailDto;
import com.rdp.service.InventoryAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/inventory-audits")
@RequiredArgsConstructor
public class InventoryAuditController {

    private final InventoryAuditService auditService;

    /**
     * Export inventory for a category
     */
    @PostMapping("/export-by-category")
    public ResponseEntity<?> exportByCategory(
            @RequestParam Long categoryId,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        try {
            log.info("Export request for category: {}", categoryId);
            
            // Get user ID from token
            Long userId = extractUserIdFromAuth(authentication);
            
            InventoryAuditDto audit = auditService.exportByCategory(categoryId, notes, userId);
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("audit", audit);
                put("message", "Category exported successfully");
            }});
        } catch (Exception e) {
            log.error("Export failed", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * Get audit details
     */
    @GetMapping("/{auditId}")
    public ResponseEntity<?> getAudit(@PathVariable Long auditId) {
        try {
            log.info("Getting audit details: {}", auditId);
            InventoryAuditDto audit = auditService.getAudit(auditId);
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("audit", audit);
            }});
        } catch (Exception e) {
            log.error("Failed to get audit", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * Upload and process audit adjustments
     */
    @PostMapping("/{auditId}/upload")
    public ResponseEntity<?> uploadAdjustments(
            @PathVariable Long auditId,
            @RequestBody List<Map<String, Object>> auditData,
            Authentication authentication) {
        try {
            log.info("Processing upload for audit: {}", auditId);
            
            Long userId = extractUserIdFromAuth(authentication);
            Map<String, Object> result = auditService.uploadAdjustments(auditId, auditData, userId);
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("result", result);
                put("message", "Adjustments processed successfully");
            }});
        } catch (Exception e) {
            log.error("Upload processing failed", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * Get audit history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam(defaultValue = "50") int limit) {
        try {
            log.info("Getting audit history, limit: {}", limit);
            List<InventoryAuditDto> audits = auditService.getHistory(limit);
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("audits", audits);
                put("count", audits.size());
            }});
        } catch (Exception e) {
            log.error("Failed to get history", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * Get variance report for audit
     */
    @GetMapping("/{auditId}/variance-report")
    public ResponseEntity<?> getVarianceReport(@PathVariable Long auditId) {
        try {
            log.info("Getting variance report for audit: {}", auditId);
            List<InventoryAuditDetailDto> variances = auditService.getVarianceReport(auditId);
            
            int totalVariance = variances.stream()
                    .mapToInt(InventoryAuditDetailDto::variance)
                    .sum();
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("variances", variances);
                put("count", variances.size());
                put("totalVariance", totalVariance);
            }});
        } catch (Exception e) {
            log.error("Failed to get variance report", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * Rollback audit adjustments
     */
    @PostMapping("/{auditId}/rollback")
    public ResponseEntity<?> rollback(@PathVariable Long auditId) {
        try {
            log.info("Rollback request for audit: {}", auditId);
            
            Map<String, Object> result = auditService.rollback(auditId);
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("result", result);
                put("message", "Audit rolled back successfully");
            }});
        } catch (Exception e) {
            log.error("Rollback failed", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * Extract user ID from authentication
     */
    private Long extractUserIdFromAuth(Authentication authentication) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                // For JWT tokens, the principal usually contains user info
                Object principal = authentication.getPrincipal();
                
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                    log.info("Authenticated user: {}", username);
                    // You can query user repository if needed, for now use placeholder
                    return 1L;
                } else if (principal instanceof String) {
                    log.info("Principal is string: {}", principal);
                    return 1L;
                }
                
                log.warn("Could not extract user from principal: {}", principal);
                return 1L; // Default user for now
            }
            log.warn("Authentication not present or not authenticated");
            return 1L; // Default user for testing
        } catch (Exception e) {
            log.error("Error extracting user from auth", e);
            return 1L; // Default fallback
        }
    }
}
