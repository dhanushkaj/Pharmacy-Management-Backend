package com.rdp.dto;

import com.rdp.model.AlertConfig;
import com.rdp.model.AlertLog;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {
    
    private Long alertLogId;
    private AlertConfig.AlertType alertType;
    private AlertConfig.AlertSeverity severity;
    private Long productId;
    private String productCode;
    private String productName;
    private String message;
    private LocalDate expiryDate;
    private Integer daysUntilExpiry;
    private Integer currentStock;
    private AlertLog.AlertStatus status;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;
    private String categoryName;
    private String supplierName;
}
