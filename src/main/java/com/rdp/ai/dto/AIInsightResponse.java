package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Response DTO for AI insights
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIInsightResponse {
    private String insight; // AI-generated text insight
    private String summary; // Brief summary
    private Map<String, Object> data; // Raw data for charts
    private String chartType; // "pie", "bar", "line", "table"
    private String timestamp;
    private boolean success;
    private String error; // Error message if any
}
