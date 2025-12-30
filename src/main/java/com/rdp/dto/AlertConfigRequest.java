package com.rdp.dto;

import com.rdp.model.AlertConfig;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfigRequest {
    
    private AlertConfig.AlertType alertType;
    private Integer thresholdDays;
    private AlertConfig.AlertSeverity severity;
    private Boolean enabled;
    private String description;
}
