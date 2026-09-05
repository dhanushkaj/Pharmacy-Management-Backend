package com.rdp.ai.controller;

import com.rdp.ai.dto.AIInsightRequest;
import com.rdp.ai.dto.AIInsightResponse;
import com.rdp.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for AI insights endpoints
 * Base path: /api/ai
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    /**
     * Generate AI insight from natural language query
     * POST /api/ai/insights
     */
    @PostMapping("/insights")
    public ResponseEntity<AIInsightResponse> generateInsight(@RequestBody AIInsightRequest request) {
        log.info("Received AI insight request: {}", request.getQuery());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    AIInsightResponse.builder()
                            .success(false)
                            .error("Query cannot be empty")
                            .build()
            );
        }

        // Default insight type if not provided
        if (request.getInsightType() == null) {
            request.setInsightType("general");
        }

        // Default time range if not provided
        if (request.getTimeRange() == null) {
            request.setTimeRange("month");
        }

        AIInsightResponse response = aiService.generateInsight(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Sales analysis endpoint
     * POST /api/ai/sales-analysis
     */
    @PostMapping("/sales-analysis")
    public ResponseEntity<AIInsightResponse> analyzeSales(
            @RequestParam(defaultValue = "month") String timeRange) {
        
        AIInsightRequest request = AIInsightRequest.builder()
                .query("Analyze sales data for " + timeRange)
                .insightType("sales")
                .timeRange(timeRange)
                .build();

        return ResponseEntity.ok(aiService.generateInsight(request));
    }

    /**
     * Inventory analysis endpoint
     * GET /api/ai/inventory-analysis
     */
    @GetMapping("/inventory-analysis")
    public ResponseEntity<AIInsightResponse> analyzeInventory() {
        AIInsightRequest request = AIInsightRequest.builder()
                .query("Analyze current inventory levels and identify low stock items")
                .insightType("inventory")
                .timeRange("today")
                .build();

        return ResponseEntity.ok(aiService.generateInsight(request));
    }

    /**
     * Customer insights endpoint
     * GET /api/ai/customer-insights
     */
    @GetMapping("/customer-insights")
    public ResponseEntity<AIInsightResponse> getCustomerInsights() {
        AIInsightRequest request = AIInsightRequest.builder()
                .query("Identify top customers and purchasing patterns")
                .insightType("customers")
                .timeRange("month")
                .build();

        return ResponseEntity.ok(aiService.generateInsight(request));
    }

    /**
     * Billing analysis endpoint
     * GET /api/ai/billing-analysis
     */
    @GetMapping("/billing-analysis")
    public ResponseEntity<AIInsightResponse> analyzeBilling(
            @RequestParam(defaultValue = "week") String timeRange) {
        
        AIInsightRequest request = AIInsightRequest.builder()
                .query("Analyze billing trends and transactions")
                .insightType("billing")
                .timeRange(timeRange)
                .build();

        return ResponseEntity.ok(aiService.generateInsight(request));
    }

    /**
     * Health check endpoint
     * GET /api/ai/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Module is running");
    }
}
