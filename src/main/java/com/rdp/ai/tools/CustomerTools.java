package com.rdp.ai.tools;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.rdp.ai.dto.CustomerSpendDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Tools for customer-related queries
 * Available methods can be called by AIService based on query context
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerTools {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get top customers ranked by total spend or purchase frequency.
     * Useful for customer analysis, loyalty programs, and revenue concentration.
     */
    @Tool(description = "Get top customers ranked by total spend. Pass limit (default 10, max 100) to get top N customers with their spend, visit count, last purchase date, and loyalty status")
    public List<CustomerSpendDTO> getTopCustomers(int limit) {
        try {
            if (limit <= 0 || limit > 100) limit = 10;
            
            String sql = "SELECT " +
                    "b.customer_id, " +
                    "c.name, " +
                    "SUM(b.grand_total) as total_spend, " +
                    "MAX(b.billing_date) as last_visit_date, " +
                    "COUNT(DISTINCT b.billing_id) as visit_count, " +
                    "AVG(b.grand_total) as avg_transaction " +
                    "FROM pharmacy.rdp_billings b " +
                    "LEFT JOIN pharmacy.rdp_customers c ON b.customer_id = c.customer_id " +
                    "GROUP BY b.customer_id, c.name " +
                    "ORDER BY total_spend DESC " +
                    "LIMIT ? ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, limit);
            List<CustomerSpendDTO> customers = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            // DEBUG: Log what we're getting from the database
            log.info("getTopCustomers returned {} records", results.size());
            if (!results.isEmpty()) {
                log.info("First record keys: {}", results.get(0).keySet());
                log.info("First record: {}", results.get(0));
            }
            
            for (Map<String, Object> row : results) {
                LocalDate lastVisit = convertToLocalDate(row.get("last_visit_date"), today);
                int daysSinceVisit = (int) ChronoUnit.DAYS.between(lastVisit, today);
                
                String loyaltyStatus = "REGULAR";
                double spend = ((Number) row.getOrDefault("total_spend", 0)).doubleValue();
                if (spend > 10000) loyaltyStatus = "HIGH_VALUE";
                else if (daysSinceVisit > 60) loyaltyStatus = "AT_RISK";
                else if (daysSinceVisit > 90) loyaltyStatus = "INACTIVE";
                
                // Get customer name from "name" column (not customer_name alias)
                String customerName = null;
                
                // Try multiple possible column names
                for (String colName : new String[]{"name", "customer_name", "NAME", "CUSTOMER_NAME"}) {
                    Object value = row.get(colName);
                    if (value != null) {
                        customerName = (String) value;
                        log.debug("Found customer name in column '{}': {}", colName, customerName);
                        break;
                    }
                }
                
                // If still null, get first string value from row
                if (customerName == null) {
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getValue() instanceof String && !entry.getKey().equals("customer_id")) {
                            customerName = (String) entry.getValue();
                            log.debug("Found customer name as fallback in column '{}': {}", entry.getKey(), customerName);
                            break;
                        }
                    }
                }
                
                log.debug("Adding customer: name={}, spend={}", customerName, spend);
                
                customers.add(new CustomerSpendDTO(
                    customerName,
                    spend,
                    lastVisit,
                    ((Number) row.getOrDefault("visit_count", 0)).intValue(),
                    ((Number) row.getOrDefault("avg_transaction", 0)).doubleValue(),
                    daysSinceVisit,
                    loyaltyStatus
                ));
            }
            
            return customers;
        } catch (Exception e) {
            log.error("Error getting top customers", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get customers who haven't made a purchase in a specified number of days.
     * Useful for identifying at-risk customers for re-engagement campaigns.
     */
    @Tool(description = "Get inactive customers who haven't purchased in N days (default 60). Returns customers with their total spend, visit count, and last purchase date")
    public List<CustomerSpendDTO> getInactiveCustomers(int daysSinceLastVisit) {
        try {
            if (daysSinceLastVisit <= 0) daysSinceLastVisit = 60;
            
            // Calculate cutoff date
            LocalDate cutoffDate = LocalDate.now().minusDays(daysSinceLastVisit);
            
            String sql = "SELECT " +
                    "c.customer_id, " +
                    "c.name, " +
                    "SUM(b.grand_total) as total_spend, " +
                    "MAX(b.billing_date) as last_visit_date, " +
                    "COUNT(DISTINCT b.billing_id) as visit_count, " +
                    "AVG(b.grand_total) as avg_transaction " +
                    "FROM pharmacy.rdp_customers c " +
                    "LEFT JOIN pharmacy.rdp_billings b ON c.customer_id = b.customer_id " +
                    "GROUP BY c.customer_id, c.name " +
                    "HAVING MAX(b.billing_date) < ? " +
                    "ORDER BY MAX(b.billing_date) DESC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, cutoffDate);
            List<CustomerSpendDTO> customers = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            for (Map<String, Object> row : results) {
                LocalDate lastVisit = convertToLocalDate(row.get("last_visit_date"), today);
                
                // Get customer name from "name" column
                String customerName = null;
                
                // Try multiple possible column names
                for (String colName : new String[]{"name", "customer_name", "NAME", "CUSTOMER_NAME"}) {
                    Object value = row.get(colName);
                    if (value != null) {
                        customerName = (String) value;
                        log.debug("Found customer name in column '{}': {}", colName, customerName);
                        break;
                    }
                }
                
                // If still null, get first string value from row
                if (customerName == null) {
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getValue() instanceof String && !entry.getKey().equals("customer_id")) {
                            customerName = (String) entry.getValue();
                            log.debug("Found customer name as fallback in column '{}': {}", entry.getKey(), customerName);
                            break;
                        }
                    }
                }
                
                customers.add(new CustomerSpendDTO(
                    customerName,
                    ((Number) row.getOrDefault("total_spend", 0)).doubleValue(),
                    lastVisit,
                    ((Number) row.getOrDefault("visit_count", 0)).intValue(),
                    ((Number) row.getOrDefault("avg_transaction", 0)).doubleValue(),
                    (int) ChronoUnit.DAYS.between(lastVisit, today),
                    "INACTIVE"
                ));
            }
            
            return customers;
        } catch (Exception e) {
            log.error("Error getting inactive customers", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get recent purchase history for a specific customer by name
     */
   @Tool(description = "Get purchase history for a specific customer by name. Shows recent transactions, items purchased, quantities, and amounts")
    public Map<String, Object> getCustomerPurchaseHistory(String customerName) {
        try {
            String sql = "SELECT " +
                    "c.name as customer_name, " +
                    "b.billing_date, " +
                    "b.grand_total, " +
                    "p.name as product_name, " +
                    "bi.quantity, " +
                    "bi.subtotal " +
                    "FROM pharmacy.rdp_billings b " +
                    "LEFT JOIN pharmacy.rdp_customers c ON b.customer_id = c.customer_id " +
                    "LEFT JOIN pharmacy.rdp_billing_items bi ON b.billing_id = bi.billing_id " +
                    "LEFT JOIN pharmacy.rdp_products p ON bi.product_id = p.product_id " +
                    "WHERE LOWER(c.name) LIKE ? " +
                    "ORDER BY b.billing_date DESC " +
                    "LIMIT 20 ";
            
            String searchTerm = "%" + customerName.toLowerCase() + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, searchTerm);
            
            return Map.of(
                "customerName", customerName,
                "purchaseHistory", results,
                "recordCount", results.size()
            );
        } catch (Exception e) {
            log.error("Error getting customer purchase history", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Helper method to convert database date/timestamp to LocalDate
     * Handles both java.sql.Date and java.sql.Timestamp
     */
    private LocalDate convertToLocalDate(Object dateObj, LocalDate defaultDate) {
        if (dateObj == null) {
            return defaultDate;
        }
        
        try {
            if (dateObj instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateObj).toLocalDateTime().toLocalDate();
            } else if (dateObj instanceof java.sql.Date) {
                return ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof LocalDate) {
                return (LocalDate) dateObj;
            }
        } catch (Exception e) {
            log.warn("Error converting date object: {}", dateObj, e);
        }
        
        return defaultDate;
    }
}
