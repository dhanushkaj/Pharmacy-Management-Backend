package com.rdp.service;

import com.rdp.dto.*;
import com.rdp.model.*;
import com.rdp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertConfigRepository alertConfigRepository;
    private final AlertLogRepository alertLogRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Scheduled job to check for expiring products daily at 8:00 AM
     */
    @Scheduled(cron = "${alert.schedule.cron:0 0 8 * * ?}")
    @Transactional
    public void generateDailyAlerts() {
        log.info("Starting daily alert generation...");
        try {
            generateExpiryAlerts();
            log.info("Daily alert generation completed successfully");
        } catch (Exception e) {
            log.error("Error generating daily alerts", e);
        }
    }

    /**
     * Manual trigger for alert generation
     */
    @Transactional
    public void generateExpiryAlerts() {
        LocalDate today = LocalDate.now();
        
        // Get enabled alert configurations
        List<AlertConfig> configs = alertConfigRepository.findByEnabledTrueOrderByThresholdDaysAsc();
        
        if (configs.isEmpty()) {
            log.warn("No enabled alert configurations found. Using defaults.");
            useDefaultConfigurations();
            configs = alertConfigRepository.findByEnabledTrueOrderByThresholdDaysAsc();
        }

        // Get all products
        List<Product> products = productRepository.findAll();
        int alertsGenerated = 0;

        // Prepare configs by type for quick lookup
        AlertConfig expiryCriticalConfig = configs.stream().filter(c -> c.getAlertType() == AlertConfig.AlertType.EXPIRY_CRITICAL).findFirst().orElse(null);
        AlertConfig expiryWarningConfig = configs.stream().filter(c -> c.getAlertType() == AlertConfig.AlertType.EXPIRY_WARNING).findFirst().orElse(null);
        AlertConfig lowStockConfig = configs.stream().filter(c -> c.getAlertType() == AlertConfig.AlertType.LOW_STOCK).findFirst().orElse(null);
        AlertConfig outOfStockConfig = configs.stream().filter(c -> c.getAlertType() == AlertConfig.AlertType.OUT_OF_STOCK).findFirst().orElse(null);

        for (Product product : products) {
            // --- Expiry Alerts ---
            if (product.getExpiryDate() != null) {
                long daysUntilExpiry = ChronoUnit.DAYS.between(today, product.getExpiryDate());
                if (expiryCriticalConfig != null && daysUntilExpiry <= expiryCriticalConfig.getThresholdDays() && daysUntilExpiry >= 0) {
                    createStockOrExpiryAlertIfNotExists(product, expiryCriticalConfig, daysUntilExpiry);
                } else if (expiryWarningConfig != null && daysUntilExpiry <= expiryWarningConfig.getThresholdDays() && daysUntilExpiry >= 0) {
                    createStockOrExpiryAlertIfNotExists(product, expiryWarningConfig, daysUntilExpiry);
                }
                if (daysUntilExpiry < 0) {
                    resolveProductAlerts(product.getProductId());
                }
            }

            // --- Stock Alerts ---
            int currentStock = getCurrentStock(product);
            if (outOfStockConfig != null && currentStock == 0) {
                createStockOrExpiryAlertIfNotExists(product, outOfStockConfig, null);
            } else if (lowStockConfig != null && product.getMinStock() != null && currentStock < product.getMinStock() && currentStock > 0) {
                createStockOrExpiryAlertIfNotExists(product, lowStockConfig, null);
            }
        }
        log.info("Generated {} new alerts", alertsGenerated);
    }

    // Helper to avoid duplicate alerts
    private void createStockOrExpiryAlertIfNotExists(Product product, AlertConfig config, Long daysUntilExpiry) {
        List<AlertLog> existingAlerts = alertLogRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
            product.getProductId(), AlertLog.AlertStatus.ACTIVE);
        boolean alertExists = existingAlerts.stream().anyMatch(a -> a.getSeverity() == config.getSeverity() && a.getAlertType() == config.getAlertType());
        if (!alertExists) {
            createAlertLog(product, config, daysUntilExpiry != null ? daysUntilExpiry : 0);
        }
    // removed extra closing brace
    }

    private void createAlertLog(Product product, AlertConfig config, long daysUntilExpiry) {
        String message;
        if (config.getAlertType() == AlertConfig.AlertType.OUT_OF_STOCK) {
            message = String.format("OUT OF STOCK: %s is out of stock!", product.getName());
        } else if (config.getAlertType() == AlertConfig.AlertType.LOW_STOCK) {
            message = String.format("LOW STOCK: %s stock is below minimum threshold!", product.getName());
        } else {
            message = generateAlertMessage(product, daysUntilExpiry, config.getSeverity());
        }
        
        AlertLog alertLog = AlertLog.builder()
            .alertType(config.getAlertType())
            .severity(config.getSeverity())
            .productId(product.getProductId())
            .productCode(product.getProductCode())
            .productName(product.getName())
            .message(message)
            .expiryDate(product.getExpiryDate())
            .daysUntilExpiry((int) daysUntilExpiry)
            .currentStock(getCurrentStock(product))
            .status(AlertLog.AlertStatus.ACTIVE)
            .build();
        
        alertLogRepository.save(alertLog);
    }

    private String generateAlertMessage(Product product, long daysUntilExpiry, AlertConfig.AlertSeverity severity) {
        if (daysUntilExpiry == 0) {
            return String.format("URGENT: %s expires TODAY!", product.getName());
        } else if (daysUntilExpiry == 1) {
            return String.format("%s: %s expires TOMORROW", severity, product.getName());
        } else {
            return String.format("%s: %s expires in %d days", severity, product.getName(), daysUntilExpiry);
        }
    }

    private Integer getCurrentStock(Product product) {
        // Aggregate stock from inventory items
        return inventoryItemRepository.findByProduct(product).stream()
            .mapToInt(item -> item.getStock() != null ? item.getStock() : 0)
            .sum();
    }

    @Transactional
    public void resolveProductAlerts(Long productId) {
        List<AlertLog> activeAlerts = alertLogRepository
            .findByProductIdAndStatusOrderByCreatedAtDesc(productId, AlertLog.AlertStatus.ACTIVE);
        
        activeAlerts.forEach(alert -> {
            alert.setStatus(AlertLog.AlertStatus.RESOLVED);
            alertLogRepository.save(alert);
        });
    }

    /**
     * Get alert summary for dashboard
     */
    public AlertSummaryResponse getAlertSummary() {
        Long totalActive = alertLogRepository.countByStatus(AlertLog.AlertStatus.ACTIVE);
        Long criticalCount = alertLogRepository.countByStatusAndSeverity(
            AlertLog.AlertStatus.ACTIVE, AlertConfig.AlertSeverity.CRITICAL);
        Long warningCount = alertLogRepository.countByStatusAndSeverity(
            AlertLog.AlertStatus.ACTIVE, AlertConfig.AlertSeverity.WARNING);
        Long infoCount = alertLogRepository.countByStatusAndSeverity(
            AlertLog.AlertStatus.ACTIVE, AlertConfig.AlertSeverity.INFO);
        
        return AlertSummaryResponse.builder()
            .totalActive(totalActive)
            .criticalCount(criticalCount)
            .warningCount(warningCount)
            .infoCount(infoCount)
            .build();
    }

    /**
     * Get paginated alerts with filters
     */
    public Page<AlertResponse> getAlerts(
            AlertLog.AlertStatus status,
            AlertConfig.AlertSeverity severity,
            AlertConfig.AlertType alertType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        
        Page<AlertLog> alerts = alertLogRepository.findWithFilters(
            status, severity, alertType, startDate, endDate, pageable);
        
        return alerts.map(this::toAlertResponse);
    }

    /**
     * Acknowledge an alert
     */
    @Transactional
    public AlertResponse acknowledgeAlert(Long alertLogId) {
        AlertLog alert = alertLogRepository.findById(alertLogId)
            .orElseThrow(() -> new RuntimeException("Alert not found with ID: " + alertLogId));
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        alert.setStatus(AlertLog.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(username);
        alert.setAcknowledgedAt(LocalDateTime.now());
        
        AlertLog saved = alertLogRepository.save(alert);
        return toAlertResponse(saved);
    }

    /**
     * Acknowledge multiple alerts
     */
    @Transactional
    public void acknowledgeMultipleAlerts(List<Long> alertIds) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime now = LocalDateTime.now();
        
        alertIds.forEach(id -> {
            alertLogRepository.findById(id).ifPresent(alert -> {
                alert.setStatus(AlertLog.AlertStatus.ACKNOWLEDGED);
                alert.setAcknowledgedBy(username);
                alert.setAcknowledgedAt(now);
                alertLogRepository.save(alert);
            });
        });
    }

    // ============= Alert Configuration Management =============

    public List<AlertConfigResponse> getAllConfigurations() {
        return alertConfigRepository.findAll().stream()
            .map(this::toConfigResponse)
            .toList();
    }

    public AlertConfigResponse getConfiguration(Long configId) {
        AlertConfig config = alertConfigRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("Alert configuration not found with ID: " + configId));
        return toConfigResponse(config);
    }

    @Transactional
    public AlertConfigResponse createConfiguration(AlertConfigRequest request) {
        AlertConfig config = AlertConfig.builder()
            .alertType(request.getAlertType())
            .thresholdDays(request.getThresholdDays())
            .severity(request.getSeverity())
            .enabled(request.getEnabled() != null ? request.getEnabled() : true)
            .description(request.getDescription())
            .build();
        
        AlertConfig saved = alertConfigRepository.save(config);
        return toConfigResponse(saved);
    }

    @Transactional
    public AlertConfigResponse updateConfiguration(Long configId, AlertConfigRequest request) {
        AlertConfig config = alertConfigRepository.findById(configId)
            .orElseThrow(() -> new RuntimeException("Alert configuration not found with ID: " + configId));
        
        if (request.getAlertType() != null) config.setAlertType(request.getAlertType());
        if (request.getThresholdDays() != null) config.setThresholdDays(request.getThresholdDays());
        if (request.getSeverity() != null) config.setSeverity(request.getSeverity());
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        
        AlertConfig saved = alertConfigRepository.save(config);
        return toConfigResponse(saved);
    }

    @Transactional
    public void deleteConfiguration(Long configId) {
        alertConfigRepository.deleteById(configId);
    }

    /**
     * Initialize default alert configurations if none exist
     */
    @Transactional
    public void useDefaultConfigurations() {
        if (alertConfigRepository.count() == 0) {
            log.info("Initializing default alert configurations...");
            
            // Critical: 30 days
            alertConfigRepository.save(AlertConfig.builder()
                .alertType(AlertConfig.AlertType.EXPIRY_CRITICAL)
                .thresholdDays(30)
                .severity(AlertConfig.AlertSeverity.CRITICAL)
                .enabled(true)
                .description("Products expiring within 30 days")
                .build());
            
            // Warning: 60 days
            alertConfigRepository.save(AlertConfig.builder()
                .alertType(AlertConfig.AlertType.EXPIRY_WARNING)
                .thresholdDays(60)
                .severity(AlertConfig.AlertSeverity.WARNING)
                .enabled(true)
                .description("Products expiring within 60 days")
                .build());
            
            // Info: 90 days
            alertConfigRepository.save(AlertConfig.builder()
                .alertType(AlertConfig.AlertType.EXPIRY_WARNING)
                .thresholdDays(90)
                .severity(AlertConfig.AlertSeverity.INFO)
                .enabled(true)
                .description("Products expiring within 90 days")
                .build());
            
            log.info("Default alert configurations created");
        }
    }

    // ============= Mapping Methods =============

    private AlertResponse toAlertResponse(AlertLog alert) {
        return AlertResponse.builder()
            .alertLogId(alert.getAlertLogId())
            .alertType(alert.getAlertType())
            .severity(alert.getSeverity())
            .productId(alert.getProductId())
            .productCode(alert.getProductCode())
            .productName(alert.getProductName())
            .message(alert.getMessage())
            .expiryDate(alert.getExpiryDate())
            .daysUntilExpiry(alert.getDaysUntilExpiry())
            .currentStock(alert.getCurrentStock())
            .status(alert.getStatus())
            .acknowledgedBy(alert.getAcknowledgedBy())
            .acknowledgedAt(alert.getAcknowledgedAt())
            .createdAt(alert.getCreatedAt())
            .build();
    }

    private AlertConfigResponse toConfigResponse(AlertConfig config) {
        return AlertConfigResponse.builder()
            .alertConfigId(config.getAlertConfigId())
            .alertType(config.getAlertType())
            .thresholdDays(config.getThresholdDays())
            .severity(config.getSeverity())
            .enabled(config.getEnabled())
            .description(config.getDescription())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .createdBy(config.getCreatedBy())
            .updatedBy(config.getUpdatedBy())
            .build();
    }
}
