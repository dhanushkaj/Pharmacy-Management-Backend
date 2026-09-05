package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI insights queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIInsightRequest {
    private String query;
    private String insightType; // "sales", "inventory", "customers", "billing", "general"
    private String timeRange; // "today", "week", "month", "quarter", "year"
    private String category; // Optional: specific category filter
}
