package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "rdp_alert_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfig extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_config_id")
    private Long alertConfigId;

    @NotNull(message = "Alert type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @NotNull(message = "Threshold days is required")
    @Min(value = 0, message = "Threshold days must be positive")
    @Column(name = "threshold_days", nullable = false)
    private Integer thresholdDays;

    @NotNull(message = "Severity is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @NotNull(message = "Enabled flag is required")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "description", length = 500)
    private String description;

    public enum AlertType {
        EXPIRY_WARNING,
        EXPIRY_CRITICAL,
        LOW_STOCK,
        OUT_OF_STOCK
    }

    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
}
