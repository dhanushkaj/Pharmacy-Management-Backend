package com.rdp.dto;

import com.rdp.model.AlertConfig;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfigResponse {
    
    private Long alertConfigId;
    private AlertConfig.AlertType alertType;
    private Integer thresholdDays;
    private AlertConfig.AlertSeverity severity;
    private Boolean enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
