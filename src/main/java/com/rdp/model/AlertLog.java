package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rdp_alert_logs", indexes = {
    @Index(name = "rdp_idx_alert_product", columnList = "product_id"),
    @Index(name = "rdp_idx_alert_status", columnList = "status"),
    @Index(name = "rdp_idx_alert_severity", columnList = "severity"),
    @Index(name = "rdp_idx_alert_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLog extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_log_id")
    private Long alertLogId;

    @NotNull(message = "Alert type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertConfig.AlertType alertType;

    @NotNull(message = "Severity is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertConfig.AlertSeverity severity;

    @NotNull(message = "Product ID is required")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "days_until_expiry")
    private Integer daysUntilExpiry;

    @Column(name = "current_stock")
    private Integer currentStock;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        RESOLVED,
        EXPIRED
    }
}
