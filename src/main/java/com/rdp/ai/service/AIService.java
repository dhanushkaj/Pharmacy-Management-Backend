package com.rdp.ai.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdp.ai.dto.AIInsightRequest;
import com.rdp.ai.dto.AIInsightResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for AI-powered insights
 * Integrates with Ollama for natural language processing
 * and database queries for pharmacy data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final AIToolManager aiToolManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String OLLAMA_MODEL = "mistral";

    /**
     * Clean up and format AI response for better readability
     */
    private String formatAIResponse(String text) {
        // Step 1: Clean up currency
        text = cleanupCurrencyFormatting(text);
        
        // Step 2: Break into logical sections
        text = text.replaceAll("(?i)(unfortunately|however|based on|to capitalize|actionable insights?:?)", "\n\n$1");
        
        // Step 3: Format numbered lists - convert "1. " to proper numbering
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "\n• ");
        
        // Step 4: Add spacing between sentences in continuous text
        text = text.replaceAll("(?<=\\.)\\s+(?=[A-Z])", "\n\n");
        
        // Step 5: Clean up multiple newlines
        text = text.replaceAll("\n{3,}", "\n\n");
        
        // Step 6: Trim each line
        String[] lines = text.split("\n");
        StringBuilder formatted = new StringBuilder();
        for (String line : lines) {
            formatted.append(line.trim()).append("\n");
        }
        
        return formatted.toString().trim();
    }

    /**
     * Generate AI insight based on user query
     */
    public AIInsightResponse generateInsight(AIInsightRequest request) {
        try {
            log.info("Processing AI insight request: {}", request.getQuery());

            // Step 1: Get data from database - intelligently route based on query content
            Map<String, Object> databaseData = aiToolManager.routeQuery(request.getQuery());

            // Step 2: Format data for Ollama
            String formattedData = formatDataForAI(databaseData, request);

            // Step 3: Call Ollama to generate insight
            String aiGeneratedInsight = callOllama(formattedData);
            
            log.info("AI response generated, length: {}", aiGeneratedInsight.length());
            
            // Step 4: Format the response nicely
            aiGeneratedInsight = formatAIResponse(aiGeneratedInsight);

            // Step 5: Determine chart type based on query keywords
            String chartType = determineChartTypeFromQuery(request.getQuery());

            // Build response
            return AIInsightResponse.builder()
                    .insight(aiGeneratedInsight)
                    .summary(generateSummary(aiGeneratedInsight))
                    .data(Map.of("query", request.getQuery(), "timestamp", LocalDateTime.now().toString()))
                    .chartType(chartType)
                    .timestamp(LocalDateTime.now().toString())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error generating insight", e);
            return AIInsightResponse.builder()
                    .insight("Error generating insight: " + e.getMessage())
                    .success(false)
                    .timestamp(LocalDateTime.now().toString())
                    .build();
        }
    }

    /**
     * Clean up currency formatting - replace $ with RS
     */
    private String cleanupCurrencyFormatting(String text) {
        // Replace $X with RS X
        text = text.replaceAll("\\$([0-9,]+(?:\\.[0-9]{2})?)", "RS $1");
        return text;
    }

    /**
     * Detect specific month from query string
     * Returns month number (1-12) or -1 if not found
     */
    private int detectMonthFromQuery(String query) {
        String queryLower = query.toLowerCase();
        String[] months = {"january", "february", "march", "april", "may", "june",
                          "july", "august", "september", "october", "november", "december",
                          "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        int[] monthNumbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                             1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        
        for (int i = 0; i < months.length; i++) {
            if (queryLower.contains(months[i])) {
                return monthNumbers[i];
            }
        }
        return -1;
    }

    /**
     * Fetch data from database based on insight type
     */
    private Map<String, Object> fetchDataByType(AIInsightRequest request) {
        Map<String, Object> data = new HashMap<>();
        
        // Check query intent for user/customer distinction
        String queryLower = request.getQuery().toLowerCase();
        boolean isAskingAboutUsers = queryLower.contains("registered") || queryLower.contains("users") || 
                                     queryLower.contains("staff") || queryLower.contains("employees") ||
                                     queryLower.contains("how many users");

        // Check if query is asking for specific month
        int monthDetected = detectMonthFromQuery(request.getQuery());
        if (monthDetected != -1) {
            // Fetch month-specific comprehensive sales data
            data.put("monthlyAnalysis", fetchMonthSpecificSalesAnalysis(monthDetected));
            return data;
        }

        switch (request.getInsightType().toLowerCase()) {
            case "sales":
                data.put("salesData", fetchSalesData(request.getTimeRange()));
                break;
            case "inventory":
                data.put("inventoryData", fetchInventoryData());
                break;
            case "customers":
                // If query is about registered users, fetch from rdp_users
                // Otherwise fetch customer/buyer data from rdp_customers
                if (isAskingAboutUsers) {
                    data.put("userData", fetchRegisteredUsers());
                } else {
                    data.put("customerData", fetchCustomerData());
                }
                break;
            case "billing":
                data.put("billingData", fetchBillingData(request.getTimeRange()));
                break;
            default:
                // For general queries, check if they're asking about users
                if (isAskingAboutUsers) {
                    data.put("userData", fetchRegisteredUsers());
                } else {
                    data.put("generalData", "Sample pharmacy data");
                }
        }

        return data;
    }

    /**
     * Fetch registered users from rdp_users table
     */
    private Map<String, Object> fetchRegisteredUsers() {
        try {
            String sql = "SELECT " +
                    "u.user_id, " +
                    "u.username, " +
                    "u.email, " +
                    "u.created_at, " +
                    "u.role " +
                    "FROM rdp_users u " +
                    "ORDER BY u.created_at DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("totalUsers", results.size());
            
            // Count by role
            long adminCount = results.stream()
                    .filter(r -> "admin".equalsIgnoreCase(String.valueOf(r.get("role"))))
                    .count();
            long staffCount = results.size() - adminCount;
            
            response.put("adminCount", adminCount);
            response.put("staffCount", staffCount);
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching registered users", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch sales data from database
     */
    private Map<String, Object> fetchSalesData(String timeRange) {
        try {
            String sql = buildSalesSql(timeRange);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("count", results.size());
            
            // Add monthly breakdown
            response.put("monthlyBreakdown", fetchMonthlySalesBreakdown());
            
            // Add yearly comparison
            response.put("yearlyComparison", fetchYearlySalesComparison());
            
            // Add Q2Q (Quarter-to-Quarter) sales by category
            response.put("q2qByCategory", fetchQ2QSalesByCategory());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching sales data", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch monthly sales breakdown for trend analysis
     */
    private Map<String, Object> fetchMonthlySalesBreakdown() {
        try {
            String sql = "SELECT " +
                    "TO_CHAR(b.billing_date, 'YYYY-MM') as month, " +
                    "COUNT(*) as transaction_count, " +
                    "COALESCE(SUM(b.grand_total), 0) as monthly_total, " +
                    "COALESCE(SUM(bi.quantity), 0) as total_items " +
                    "FROM rdp_billings b " +
                    "LEFT JOIN rdp_billing_items bi ON b.billing_id = bi.billing_id " +
                    "WHERE b.billing_date >= CURRENT_DATE - INTERVAL '12 months' " +
                    "GROUP BY TO_CHAR(b.billing_date, 'YYYY-MM') " +
                    "ORDER BY month DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            
            if (!results.isEmpty()) {
                // Calculate trend (is it going up or down)
                double firstMonth = ((Number) results.get(0).get("monthly_total")).doubleValue();
                double lastMonth = ((Number) results.get(Math.min(results.size() - 1, 11)).get("monthly_total")).doubleValue();
                double trend = ((firstMonth - lastMonth) / lastMonth) * 100;
                response.put("trendPercentage", String.format("%.1f%%", trend));
                response.put("trendDirection", trend > 0 ? "UP" : trend < 0 ? "DOWN" : "STABLE");
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching monthly sales breakdown", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch yearly sales comparison
     */
    private Map<String, Object> fetchYearlySalesComparison() {
        try {
            String sql = "SELECT " +
                    "TO_CHAR(b.billing_date, 'YYYY') as year, " +
                    "COUNT(*) as transaction_count, " +
                    "COALESCE(SUM(b.grand_total), 0) as yearly_total " +
                    "FROM rdp_billings b " +
                    "GROUP BY TO_CHAR(b.billing_date, 'YYYY') " +
                    "ORDER BY year DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("yearsAvailable", results.size());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching yearly sales comparison", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch Q2Q (Quarter-to-Quarter) sales by category
     * Shows Q1, Q2, Q3, Q4 sales breakdown for each product category
     */
    private Map<String, Object> fetchQ2QSalesByCategory() {
        try {
            // Query to get quarterly sales by product category
            String sql = "SELECT " +
                    "COALESCE(pc.name, p.name, 'Uncategorized') as category, " +
                    "SUM(CASE WHEN EXTRACT(QUARTER FROM b.billing_date) = 1 " +
                    "    THEN COALESCE(bi.subtotal, 0) ELSE 0 END) as q1_sales, " +
                    "SUM(CASE WHEN EXTRACT(QUARTER FROM b.billing_date) = 2 " +
                    "    THEN COALESCE(bi.subtotal, 0) ELSE 0 END) as q2_sales, " +
                    "SUM(CASE WHEN EXTRACT(QUARTER FROM b.billing_date) = 3 " +
                    "    THEN COALESCE(bi.subtotal, 0) ELSE 0 END) as q3_sales, " +
                    "SUM(CASE WHEN EXTRACT(QUARTER FROM b.billing_date) = 4 " +
                    "    THEN COALESCE(bi.subtotal, 0) ELSE 0 END) as q4_sales, " +
                    "SUM(COALESCE(bi.subtotal, 0)) as total_sales, " +
                    "COUNT(DISTINCT b.billing_id) as total_transactions " +
                    "FROM rdp_billing_items bi " +
                    "LEFT JOIN rdp_billings b ON bi.billing_id = b.billing_id " +
                    "LEFT JOIN rdp_products p ON bi.product_id = p.product_id " +
                    "LEFT JOIN rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE b.billing_date IS NOT NULL " +
                    "GROUP BY COALESCE(pc.name, p.name, 'Uncategorized') " +
                    "ORDER BY total_sales DESC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("categoriesAnalyzed", results.size());
            
            // Calculate best performing quarter overall and monthly breakdown
            if (!results.isEmpty()) {
                double q1Total = 0, q2Total = 0, q3Total = 0, q4Total = 0;
                for (Map<String, Object> row : results) {
                    q1Total += ((Number) row.getOrDefault("q1_sales", 0)).doubleValue();
                    q2Total += ((Number) row.getOrDefault("q2_sales", 0)).doubleValue();
                    q3Total += ((Number) row.getOrDefault("q3_sales", 0)).doubleValue();
                    q4Total += ((Number) row.getOrDefault("q4_sales", 0)).doubleValue();
                }
                
                response.put("QUARTER_TOTALS", String.format("Q1: RS %.2f | Q2: RS %.2f | Q3: RS %.2f | Q4: RS %.2f", 
                        q1Total, q2Total, q3Total, q4Total));
                response.put("q1_total", String.format("%.2f", q1Total));
                response.put("q2_total", String.format("%.2f", q2Total));
                response.put("q3_total", String.format("%.2f", q3Total));
                response.put("q4_total", String.format("%.2f", q4Total));
                
                // Identify best quarter
                double maxSales = Math.max(Math.max(q1Total, q2Total), Math.max(q3Total, q4Total));
                String bestQuarter = "Q1";
                if (maxSales == q2Total) bestQuarter = "Q2";
                else if (maxSales == q3Total) bestQuarter = "Q3";
                else if (maxSales == q4Total) bestQuarter = "Q4";
                
                response.put("bestQuarter", bestQuarter);
                response.put("bestQuarterAmount", String.format("%.2f", maxSales));
            }
            
            // Add monthly breakdown by category
            response.put("monthlyByCategory", fetchMonthlySalesByCategory());
            
            // Add this month's category breakdown
            response.put("thisMonthByCategory", fetchThisMonthSalesByCategory());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching Q2Q sales by category", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch monthly sales breakdown by product category
     */
    private Map<String, Object> fetchMonthlySalesByCategory() {
        try {
            String sql = "SELECT " +
                    "TO_CHAR(b.billing_date, 'YYYY-MM') as month, " +
                    "COALESCE(pc.name, p.name, 'Uncategorized') as category, " +
                    "SUM(COALESCE(bi.subtotal, 0)) as monthly_sales, " +
                    "COUNT(DISTINCT b.billing_id) as transactions " +
                    "FROM rdp_billing_items bi " +
                    "LEFT JOIN rdp_billings b ON bi.billing_id = b.billing_id " +
                    "LEFT JOIN rdp_products p ON bi.product_id = p.product_id " +
                    "LEFT JOIN rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE b.billing_date >= CURRENT_DATE - INTERVAL '12 months' " +
                    "GROUP BY TO_CHAR(b.billing_date, 'YYYY-MM'), COALESCE(pc.name, p.name, 'Uncategorized') " +
                    "ORDER BY month DESC, monthly_sales DESC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("monthlyRecords", results);
            response.put("monthsAnalyzed", results.stream()
                    .map(r -> r.get("month"))
                    .distinct()
                    .count());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching monthly sales by category", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch this month's sales by product category
     */
    private Map<String, Object> fetchThisMonthSalesByCategory() {
        try {
            String sql = "SELECT " +
                    "COALESCE(pc.name, p.name, 'Uncategorized') as category, " +
                    "SUM(COALESCE(bi.subtotal, 0)) as this_month_sales, " +
                    "COUNT(DISTINCT b.billing_id) as this_month_transactions, " +
                    "COUNT(DISTINCT bi.product_id) as products_sold " +
                    "FROM rdp_billing_items bi " +
                    "LEFT JOIN rdp_billings b ON bi.billing_id = b.billing_id " +
                    "LEFT JOIN rdp_products p ON bi.product_id = p.product_id " +
                    "LEFT JOIN rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE DATE_TRUNC('month', b.billing_date) = DATE_TRUNC('month', CURRENT_DATE) " +
                    "GROUP BY COALESCE(pc.name, p.name, 'Uncategorized') " +
                    "ORDER BY this_month_sales DESC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("thisMonthRecords", results);
            response.put("categoriesThisMonth", results.size());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching this month sales by category", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Build SQL for sales data based on time range
     */
    private String buildSalesSql(String timeRange) {
        String baseSql = "SELECT " +
                "COALESCE(p.name, 'Unknown') as product_name, " +
                "COALESCE(SUM(bi.quantity), 0) as total_quantity, " +
                "COALESCE(SUM(bi.subtotal), 0) as total_amount " +
                "FROM rdp_billing_items bi " +
                "LEFT JOIN rdp_billings b ON bi.billing_id = b.billing_id " +
                "LEFT JOIN rdp_products p ON bi.product_id = p.product_id " +
                "WHERE 1=1 ";

        switch (timeRange.toLowerCase()) {
            case "today":
                baseSql += "AND DATE(b.billing_date) = CURRENT_DATE ";
                break;
            case "week":
                baseSql += "AND b.billing_date >= CURRENT_DATE - INTERVAL '7 days' ";
                break;
            case "month":
                baseSql += "AND b.billing_date >= CURRENT_DATE - INTERVAL '30 days' ";
                break;
            case "quarter":
                baseSql += "AND b.billing_date >= CURRENT_DATE - INTERVAL '90 days' ";
                break;
            case "year":
                baseSql += "AND b.billing_date >= CURRENT_DATE - INTERVAL '365 days' ";
                break;
        }

        baseSql += "GROUP BY p.name ORDER BY total_amount DESC LIMIT 20";
        return baseSql;
    }

    /**
     * Fetch inventory data
     */
    private Map<String, Object> fetchInventoryData() {
        try {
            String sql = "SELECT " +
                    "p.name as product_name, " +
                    "i.stock as quantity_on_hand, " +
                    "p.min_stock as min_stock_level, " +
                    "CASE WHEN i.stock <= COALESCE(p.min_stock, 0) THEN 'LOW' " +
                    "ELSE 'OK' END as status " +
                    "FROM rdp_inventory_items i " +
                    "LEFT JOIN rdp_products p ON i.product_id = p.product_id " +
                    "ORDER BY i.stock ASC LIMIT 50";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("lowStockCount", results.stream()
                    .filter(r -> "LOW".equals(r.get("status")))
                    .count());
            return response;
        } catch (Exception e) {
            log.error("Error fetching inventory data", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch customer data
     */
    private Map<String, Object> fetchCustomerData() {
        try {
            String sql = "SELECT " +
                    "COALESCE(c.name, 'Unknown') as customer_name, " +
                    "COUNT(b.billing_id) as total_purchases, " +
                    "COALESCE(SUM(b.grand_total), 0) as total_spent " +
                    "FROM rdp_customers c " +
                    "LEFT JOIN rdp_billings b ON c.customer_id = b.customer_id " +
                    "GROUP BY c.name " +
                    "ORDER BY total_spent DESC LIMIT 20";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("topCustomerCount", results.size());
            return response;
        } catch (Exception e) {
            log.error("Error fetching customer data", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch billing data
     */
    private Map<String, Object> fetchBillingData(String timeRange) {
        try {
            String sql = buildBillingSql(timeRange);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("totalTransactions", results.size());
            
            // Add monthly billing analysis
            response.put("monthlyBilling", fetchMonthlyBillingAnalysis());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching billing data", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch monthly billing analysis
     */
    private Map<String, Object> fetchMonthlyBillingAnalysis() {
        try {
            String sql = "SELECT " +
                    "TO_CHAR(billing_date, 'YYYY-MM') as month, " +
                    "COUNT(*) as transactions, " +
                    "SUM(grand_total) as total_amount, " +
                    "AVG(grand_total) as avg_transaction, " +
                    "SUM(COALESCE(discount_amount, 0)) as total_discounts " +
                    "FROM rdp_billings " +
                    "WHERE billing_date >= CURRENT_DATE - INTERVAL '12 months' " +
                    "GROUP BY TO_CHAR(billing_date, 'YYYY-MM') " +
                    "ORDER BY month DESC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("monthsAnalyzed", results.size());
            
            if (!results.isEmpty()) {
                double currentMonth = ((Number) results.get(0).get("total_amount")).doubleValue();
                double previousMonth = results.size() > 1 ? 
                    ((Number) results.get(1).get("total_amount")).doubleValue() : currentMonth;
                double growth = ((currentMonth - previousMonth) / previousMonth) * 100;
                response.put("monthOverMonthGrowth", String.format("%.1f%%", growth));
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching monthly billing analysis", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Build SQL for billing data
     */
    private String buildBillingSql(String timeRange) {
        String baseSql = "SELECT " +
                "DATE(billing_date) as date, " +
                "COUNT(*) as transactions, " +
                "SUM(grand_total) as total_amount, " +
                "SUM(COALESCE(discount_amount, 0)) as total_discounts " +
                "FROM rdp_billings " +
                "WHERE 1=1 ";

        switch (timeRange.toLowerCase()) {
            case "today":
                baseSql += "AND DATE(billing_date) = CURRENT_DATE ";
                break;
            case "week":
                baseSql += "AND billing_date >= CURRENT_DATE - INTERVAL '7 days' ";
                break;
            case "month":
                baseSql += "AND billing_date >= CURRENT_DATE - INTERVAL '30 days' ";
                break;
            case "quarter":
                baseSql += "AND billing_date >= CURRENT_DATE - INTERVAL '90 days' ";
                break;
        }

        baseSql += "GROUP BY DATE(billing_date) ORDER BY date DESC";
        return baseSql;
    }

    /**
     * Call Ollama API for text generation
     */
    private String callOllama(String prompt) {
        try {
            log.info("Calling Ollama with prompt length: {}", prompt.length());

            // Create Ollama API request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", OLLAMA_MODEL);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);

            // Make HTTP request to Ollama
            try {
                String url = OLLAMA_BASE_URL + "/api/generate";
                var response = restTemplate.postForObject(url, requestBody, Map.class);

                if (response != null && response.containsKey("response")) {
                    String result = (String) response.get("response");
                    log.info("Ollama response received, length: {}", result.length());
                    return result;
                }
            } catch (Exception e) {
                log.error("Error calling Ollama API: {}", e.getMessage());
            }

            return "Unable to generate insight. Ollama service may not be available.";

        } catch (Exception e) {
            log.error("Error calling Ollama", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Format database data for AI processing
     */
    private String formatDataForAI(Map<String, Object> data, AIInsightRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a pharmacy business analyst providing facts-based analysis.\n");
        prompt.append("CRITICAL RULES:\n");
        prompt.append("1. Use ONLY the data provided below - do NOT make up or estimate any numbers\n");
        prompt.append("2. Use EXACT customer names from the data - NEVER use 'Customer A', 'Customer B', or generic labels\n");
        prompt.append("3. Report EXACT numbers with no rounding or modification\n");
        prompt.append("4. Format all currency as 'RS X,XXX' (Rupees)\n");
        prompt.append("5. If a customer name is NULL or empty, write 'Unknown Customer' - NOT a generic label\n\n");
        prompt.append("DATA PROVIDED:\n");
        prompt.append("==================\n\n");
        
        data.forEach((key, value) -> {
            // Handle topCustomers or inactiveCustomers (List of CustomerSpendDTO)
            if (key.equals("topCustomers") && value instanceof List) {
                List<?> customers = (List<?>) value;
                prompt.append("\n>>> TOP CUSTOMERS BY TOTAL SPEND:\n");
                prompt.append("    Total customers analyzed: ").append(customers.size()).append("\n\n");
                int limit = Math.min(10, customers.size());
                for (int i = 0; i < limit; i++) {
                    Object customer = customers.get(i);
                    if (customer instanceof com.rdp.ai.dto.CustomerSpendDTO) {
                        com.rdp.ai.dto.CustomerSpendDTO dto = (com.rdp.ai.dto.CustomerSpendDTO) customer;
                        // EXPLICITLY include the real customer name, NOT generic labels
                        prompt.append(String.format("    #%d - %s: RS %.0f spent, %d visits, last visit: %s, status: %s\n",
                            i + 1,
                            dto.getCustomerName() != null ? dto.getCustomerName() : "Unknown",
                            dto.getTotalSpend(),
                            dto.getVisitCount(),
                            dto.getLastVisitDate(),
                            dto.getLoyaltyStatus()));
                    } else {
                        prompt.append("    ").append(i + 1).append(". ").append(customer.toString()).append("\n");
                    }
                }
                if (customers.size() > 10) {
                    prompt.append("\n    ... and ").append(customers.size() - 10).append(" more customers in the database\n");
                }
            } 
            // Handle inactiveCustomers
            else if (key.equals("inactiveCustomers") && value instanceof List) {
                List<?> customers = (List<?>) value;
                prompt.append("\n>>> AT-RISK/INACTIVE CUSTOMERS (No purchases in last 60+ days):\n");
                prompt.append("    Total inactive customers: ").append(customers.size()).append("\n\n");
                int limit = Math.min(10, customers.size());
                for (int i = 0; i < limit; i++) {
                    Object customer = customers.get(i);
                    if (customer instanceof com.rdp.ai.dto.CustomerSpendDTO) {
                        com.rdp.ai.dto.CustomerSpendDTO dto = (com.rdp.ai.dto.CustomerSpendDTO) customer;
                        // EXPLICITLY include the real customer name
                        prompt.append(String.format("    #%d - %s: RS %.0f lifetime spend, %d visits, last purchase: %s ago\n",
                            i + 1,
                            dto.getCustomerName() != null ? dto.getCustomerName() : "Unknown",
                            dto.getTotalSpend(),
                            dto.getVisitCount(),
                            dto.getDaysSinceLastVisit()));
                    } else {
                        prompt.append("    ").append(i + 1).append(". ").append(customer.toString()).append("\n");
                    }
                }
                if (customers.size() > 10) {
                    prompt.append("\n    ... and ").append(customers.size() - 10).append(" more inactive customers\n");
                }
            }
            // Handle lowStockItems
            else if (key.equals("lowStockItems") && value instanceof List) {
                List<?> items = (List<?>) value;
                prompt.append("\n>>> LOW STOCK ITEMS:\n");
                prompt.append("    Total: ").append(items.size()).append(" products below reorder level\n");
                int limit = Math.min(10, items.size());
                for (int i = 0; i < limit; i++) {
                    prompt.append("    ").append(i + 1).append(". ").append(items.get(i).toString()).append("\n");
                }
                if (items.size() > 10) {
                    prompt.append("    ... and ").append(items.size() - 10).append(" more items\n");
                }
            }
            // Handle product_sales (specific product sales metrics)
            else if (key.equals("product_sales") && value instanceof Map) {
                Map<String, Object> productData = (Map<String, Object>) value;
                
                // Check if it's an error
                if (productData.containsKey("error")) {
                    prompt.append("\n>>> PRODUCT SALES DATA:\n");
                    prompt.append("    ").append(productData.get("error")).append("\n");
                } else {
                    prompt.append("\n>>> PRODUCT SALES METRICS:\n");
                    prompt.append("    Product: ").append(productData.getOrDefault("product_name", "Unknown")).append("\n");
                    prompt.append("    Period: ").append(productData.getOrDefault("period", "Not specified")).append("\n");
                    prompt.append("    Total Units Sold: ").append(productData.getOrDefault("total_units_sold", 0)).append(" units\n");
                    prompt.append("    Total Revenue: RS ").append(productData.getOrDefault("total_revenue", "0.00")).append("\n");
                    prompt.append("    Unit Price: RS ").append(productData.getOrDefault("unit_price", "0.00")).append("\n");
                    prompt.append("    Price Range: RS ").append(productData.getOrDefault("min_unit_price", "0.00"));
                    prompt.append(" to RS ").append(productData.getOrDefault("max_unit_price", "0.00")).append("\n");
                    prompt.append("    Total Transactions: ").append(productData.getOrDefault("total_transactions", 0)).append("\n");
                    prompt.append("    Unique Customers: ").append(productData.getOrDefault("unique_customers", 0)).append("\n");
                    
                    if (productData.containsKey("avg_price_per_unit")) {
                        prompt.append("    Average Price Per Unit: RS ").append(productData.get("avg_price_per_unit")).append("\n");
                    }
                }
            }
            // Handle Map data (nested structures)
            else if (value instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) value;
                
                // Print summary counts and metrics first and clearly
                dataMap.forEach((k, v) -> {
                    if (k.contains("TOTAL") || k.contains("total") || k.contains("Total") 
                        || k.contains("count") || k.contains("Count") 
                        || k.contains("Trend") || k.contains("Growth")
                        || k.contains("QUARTER_TOTALS")
                        || k.contains("best")) {
                        prompt.append(">>> ").append(k.toUpperCase()).append(": ").append(v).append("\n");
                    }
                });
                
                // Extract and show quarterly totals prominently
                if (dataMap.containsKey("q1_total")) {
                    prompt.append("\n>>> QUARTERLY BREAKDOWN:\n");
                    prompt.append("    Q1 (Jan-Mar): RS ").append(dataMap.getOrDefault("q1_total", "0")).append("\n");
                    prompt.append("    Q2 (Apr-Jun): RS ").append(dataMap.getOrDefault("q2_total", "0")).append("\n");
                    prompt.append("    Q3 (Jul-Sep): RS ").append(dataMap.getOrDefault("q3_total", "0")).append("\n");
                    prompt.append("    Q4 (Oct-Dec): RS ").append(dataMap.getOrDefault("q4_total", "0")).append("\n");
                }
                
                // Show category records
                if (dataMap.containsKey("records")) {
                    List<?> records = (List<?>) dataMap.get("records");
                    prompt.append("\n>>> ").append(key).append(" (").append(records.size()).append(" categories):\n");
                    
                    int limit = Math.min(10, records.size());
                    for (int i = 0; i < limit; i++) {
                        prompt.append("    ").append(i + 1).append(". ").append(records.get(i).toString()).append("\n");
                    }
                    
                    if (records.size() > 10) {
                        prompt.append("    ... and ").append(records.size() - 10).append(" more categories\n");
                    }
                }
                
                // Show monthly records if available
                if (dataMap.containsKey("monthlyRecords")) {
                    List<?> monthlyRecords = (List<?>) dataMap.get("monthlyRecords");
                    prompt.append("\n>>> MONTHLY SALES BY CATEGORY (Last 12 months, top 15):\n");
                    int limit = Math.min(15, monthlyRecords.size());
                    for (int i = 0; i < limit; i++) {
                        prompt.append("    ").append(monthlyRecords.get(i).toString()).append("\n");
                    }
                }
                
                // Show this month's data
                if (dataMap.containsKey("thisMonthRecords")) {
                    List<?> thisMonthRecords = (List<?>) dataMap.get("thisMonthRecords");
                    prompt.append("\n>>> THIS MONTH'S SALES BY CATEGORY:\n");
                    for (Object record : thisMonthRecords) {
                        prompt.append("    ").append(record.toString()).append("\n");
                    }
                }
            } else {
                prompt.append(key).append(": ").append(value.toString()).append("\n");
            }
        });

        prompt.append("\n===== ANALYSIS INSTRUCTIONS =====\n");
        prompt.append("1. Use ONLY the data provided above\n");
        prompt.append("2. DO NOT make up any numbers or data\n");
        prompt.append("3. Format all amounts as: RS X,XXX or RS X,XXX.XX\n");
        prompt.append("4. If analyzing customers: Rank them by spend, visit frequency, and show their loyalty status\n");
        prompt.append("5. If analyzing inventory: Highlight critical low stock items that need immediate reorder\n");
        prompt.append("6. If analyzing quarterly data: Compare trends across quarters and identify patterns\n");
        prompt.append("7. STRUCTURE YOUR RESPONSE:\n");
        prompt.append("   - Brief overview/summary of findings\n");
        prompt.append("   - KEY FINDINGS: Numbered list (1., 2., 3., etc.)\n");
        prompt.append("   - ACTIONABLE INSIGHTS: Recommendations for business action\n");
        prompt.append("8. Use clear paragraphs with proper spacing\n");
        prompt.append("9. Answer the specific query: ").append(request.getQuery()).append("\n");
        prompt.append("\n==================\n");
        prompt.append("FINAL CRITICAL INSTRUCTIONS:\n");
        prompt.append("- MUST use customer names EXACTLY as provided in the data above\n");
        prompt.append("- PROHIBITED: Never use 'Customer A', 'Customer B', 'Customer C', or 'Customer D'\n");
        prompt.append("- PROHIBITED: Never use generic labels or ordinal descriptions like 'first', 'second', 'third'\n");
        prompt.append("- DO: Use the actual business names provided (like 'rdp pharmacy', 'sharmi', 'rohan', etc.)\n");
        prompt.append("- DO: Include the exact currency amount and number of visits from the data\n");
        prompt.append("==================\n\n");

        return prompt.toString();
    }

    /**
     * Generate summary from full insight
     */
    private String generateSummary(String insight) {
        // Take first 200 characters as summary
        if (insight.length() > 200) {
            return insight.substring(0, 200) + "...";
        }
        return insight;
    }

    /**
     * Determine appropriate chart type based on query keywords
     */
    private String determineChartTypeFromQuery(String query) {
        String queryLower = query.toLowerCase();
        
        // Monthly/quarterly queries → line chart for trends
        if (queryLower.contains("monthly") || queryLower.contains("month") || 
            queryLower.contains("quarterly") || queryLower.contains("quarter") ||
            queryLower.contains("trend") || queryLower.contains("compare")) {
            return "line";
        }
        
        // Top/best/worst queries → bar chart for ranking
        if (queryLower.contains("top") || queryLower.contains("best") || 
            queryLower.contains("worst") || queryLower.contains("ranking")) {
            return "bar";
        }
        
        // Sales/revenue breakdown → pie chart
        if (queryLower.contains("breakdown") || queryLower.contains("distribution")) {
            return "pie";
        }
        
        // Default to table
        return "table";
    }

    /**
     * Fetch comprehensive sales analysis for a specific month
     * Returns: sales by product, sales by customer, stock availability, trends
     */
    private Map<String, Object> fetchMonthSpecificSalesAnalysis(int month) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            String monthName = getMonthName(month);
            response.put("month", monthName);
            response.put("monthNumber", month);
            
            // Sales by product for this month
            response.put("salesByProduct", fetchMonthlySalesbyProduct(month));
            
            // Sales by customer for this month
            response.put("salesByCustomer", fetchMonthlySalesbyCustomer(month));
            
            // Product inventory/stock status
            response.put("productStock", fetchProductStockStatus());
            
            // Product trends (comparing this month vs previous month)
            response.put("productTrends", fetchProductTrends(month));
            
            // Monthly summary totals
            response.put("monthlySummary", fetchMonthlySummary(month));
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching month-specific sales analysis for month: {}", month, e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Get month name from month number
     */
    private String getMonthName(int month) {
        String[] months = {"", "January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return (month > 0 && month <= 12) ? months[month] : "Unknown";
    }

    /**
     * Fetch sales breakdown by product for a specific month
     */
    private Map<String, Object> fetchMonthlySalesbyProduct(int month) {
        try {
            String sql = "SELECT " +
                    "COALESCE(p.name, 'Unknown') as product_name, " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "SUM(bi.quantity) as total_quantity, " +
                    "SUM(bi.subtotal) as total_sales, " +
                    "COUNT(DISTINCT b.billing_id) as num_transactions, " +
                    "AVG(bi.subtotal) as avg_sale_per_transaction " +
                    "FROM pharmacy.rdp_billing_items bi " +
                    "LEFT JOIN pharmacy.rdp_billings b ON bi.billing_id = b.billing_id " +
                    "LEFT JOIN pharmacy.rdp_products p ON bi.product_id = p.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE EXTRACT(MONTH FROM b.billing_date) = ? " +
                    "GROUP BY p.name, pc.name " +
                    "ORDER BY total_sales DESC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, month);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("totalProducts", results.size());
            
            if (!results.isEmpty()) {
                double totalSales = 0;
                for (Map<String, Object> row : results) {
                    totalSales += ((Number) row.getOrDefault("total_sales", 0)).doubleValue();
                }
                response.put("totalSalesAmount", String.format("%.2f", totalSales));
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching monthly sales by product", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch sales breakdown by customer for a specific month
     */
    private Map<String, Object> fetchMonthlySalesbyCustomer(int month) {
        try {
            String sql = "SELECT " +
                    "COALESCE(c.name, 'Walk-in Customer') as customer_name, " +
                    "COUNT(DISTINCT b.billing_id) as num_purchases, " +
                    "SUM(b.grand_total) as total_amount_spent, " +
                    "AVG(b.grand_total) as avg_purchase_amount, " +
                    "MAX(b.billing_date) as last_purchase_date " +
                    "FROM pharmacy.rdp_billings b " +
                    "LEFT JOIN pharmacy.rdp_customers c ON b.customer_id = c.customer_id " +
                    "WHERE EXTRACT(MONTH FROM b.billing_date) = ? " +
                    "GROUP BY c.name " +
                    "ORDER BY total_amount_spent DESC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, month);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            response.put("totalCustomers", results.size());
            
            if (!results.isEmpty()) {
                double totalSpent = 0;
                for (Map<String, Object> row : results) {
                    totalSpent += ((Number) row.getOrDefault("total_amount_spent", 0)).doubleValue();
                }
                response.put("totalMonthlyRevenue", String.format("%.2f", totalSpent));
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching monthly sales by customer", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch product stock/inventory status
     */
    private Map<String, Object> fetchProductStockStatus() {
        try {
            String sql = "SELECT " +
                    "p.name as product_name, " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "i.stock as quantity_on_hand, " +
                    "COALESCE(p.min_stock, 0) as min_stock_level, " +
                    "CASE WHEN i.stock <= COALESCE(p.min_stock, 10) THEN 'LOW STOCK' " +
                    "     WHEN i.stock <= COALESCE(p.min_stock, 10) * 2 THEN 'MEDIUM' " +
                    "     ELSE 'OK' END as stock_status " +
                    "FROM pharmacy.rdp_inventory_items i " +
                    "LEFT JOIN pharmacy.rdp_products p ON i.product_id = p.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "ORDER BY i.stock ASC";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new HashMap<>();
            response.put("records", results);
            
            long lowStockCount = results.stream()
                    .filter(r -> "LOW STOCK".equals(r.get("stock_status")))
                    .count();
            response.put("lowStockProducts", lowStockCount);
            response.put("totalProducts", results.size());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching product stock status", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch product trend analysis (this month vs previous month)
     */
    private Map<String, Object> fetchProductTrends(int month) {
        try {
            String sql = "SELECT " +
                    "COALESCE(p.name, 'Unknown') as product_name, " +
                    "SUM(CASE WHEN EXTRACT(MONTH FROM b.billing_date) = ? " +
                    "         THEN bi.subtotal ELSE 0 END) as current_month_sales, " +
                    "SUM(CASE WHEN EXTRACT(MONTH FROM b.billing_date) = ? " +
                    "         THEN bi.subtotal ELSE 0 END) as previous_month_sales " +
                    "FROM pharmacy.rdp_billing_items bi " +
                    "LEFT JOIN pharmacy.rdp_billings b ON bi.billing_id = b.billing_id " +
                    "LEFT JOIN pharmacy.rdp_products p ON bi.product_id = p.product_id " +
                    "WHERE EXTRACT(MONTH FROM b.billing_date) IN (?, ?) " +
                    "GROUP BY p.name " +
                    "HAVING SUM(bi.subtotal) > 0 " +
                    "ORDER BY current_month_sales DESC ";
            
            int previousMonth = month == 1 ? 12 : month - 1;
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, month, previousMonth, month, previousMonth);
            
            Map<String, Object> response = new HashMap<>();
            // Enrich with trend direction
            List<Map<String, Object>> enrichedResults = new ArrayList<>();
            for (Map<String, Object> row : results) {
                double current = ((Number) row.getOrDefault("current_month_sales", 0)).doubleValue();
                double previous = ((Number) row.getOrDefault("previous_month_sales", 0)).doubleValue();
                
                String trend = "STABLE";
                double percentChange = 0;
                if (previous > 0) {
                    percentChange = ((current - previous) / previous) * 100;
                    trend = current > previous ? "UP ↑" : current < previous ? "DOWN ↓" : "STABLE";
                    row.put("percentChange", String.format("%.1f%%", percentChange));
                }
                row.put("trend", trend);
                enrichedResults.add(row);
            }
            
            response.put("records", enrichedResults);
            response.put("productsWithTrend", enrichedResults.size());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching product trends", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Fetch monthly summary with key metrics
     */
    private Map<String, Object> fetchMonthlySummary(int month) {
        try {
            String sql = "SELECT " +
                    "COUNT(DISTINCT b.billing_id) as total_transactions, " +
                    "COUNT(DISTINCT CASE WHEN b.customer_id IS NOT NULL THEN b.customer_id END) as unique_customers, " +
                    "SUM(b.grand_total) as total_revenue, " +
                    "AVG(b.grand_total) as avg_transaction_value, " +
                    "SUM(COALESCE(b.discount_amount, 0)) as total_discounts " +
                    "FROM rdp_billings b " +
                    "WHERE EXTRACT(MONTH FROM b.billing_date) = ? ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, month);
            Map<String, Object> response = new HashMap<>();
            
            if (!results.isEmpty()) {
                response.putAll(results.get(0));
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching monthly summary", e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }
}

