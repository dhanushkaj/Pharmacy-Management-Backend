package com.rdp.dto;

import com.rdp.model.AlertConfig;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertSummaryResponse {
    
    private Long totalActive;
    private Long criticalCount;
    private Long warningCount;
    private Long infoCount;
    private AlertConfig criticalThreshold;
    private AlertConfig warningThreshold;
    private AlertConfig infoThreshold;
}
