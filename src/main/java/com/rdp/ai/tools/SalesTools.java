package com.rdp.ai.tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.rdp.ai.dto.ProductSalesDTO;
import com.rdp.ai.dto.SalesSummaryDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Tools for sales and revenue queries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesTools {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get sales summary including total revenue, transactions, and best-selling product for a date range
     */
    @Tool(description = "Get sales summary for a date range. Returns total revenue, transaction count, average transaction value, best-selling product, and unique customer count")
    public SalesSummaryDTO getSalesSummary(String startDate, String endDate) {
        try {
            // Parse dates
            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
            
            String sql = "SELECT " +
                    "COALESCE(SUM(b.grand_total), 0) as total_revenue, " +
                    "COUNT(DISTINCT b.billing_id) as total_transactions, " +
                    "AVG(b.grand_total) as avg_transaction, " +
                    "COUNT(DISTINCT b.customer_id) as unique_customers " +
                    "FROM pharmacy.rdp_billings b " +
                    "WHERE DATE(b.billing_date) >= ? AND DATE(b.billing_date) <= ? ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, start, end);
            Map<String, Object> summary = results.get(0);
            
            // Get best-selling product
            String productSql = "SELECT " +
                    "COALESCE(p.name, 'Unknown') as product_name, " +
                    "SUM(bi.quantity) as units_sold, " +
                    "SUM(bi.subtotal) as revenue " +
                    "FROM pharmacy.rdp_billing_items bi " +
                    "LEFT JOIN pharmacy.rdp_billings b ON bi.billing_id = b.billing_id " +
                    "LEFT JOIN pharmacy.rdp_products p ON bi.product_id = p.product_id " +
                    "WHERE DATE(b.billing_date) >= ? AND DATE(b.billing_date) <= ? " +
                    "GROUP BY p.name " +
                    "ORDER BY units_sold DESC LIMIT 1 ";
            
            List<Map<String, Object>> productResults = jdbcTemplate.queryForList(productSql, start, end);
            
            return new SalesSummaryDTO(
                ((Number) summary.get("total_revenue")).doubleValue(),
                ((Number) summary.get("total_transactions")).longValue(),
                ((Number) summary.get("avg_transaction")).doubleValue(),
                productResults.isEmpty() ? "N/A" : (String) productResults.get(0).get("product_name"),
                productResults.isEmpty() ? 0 : ((Number) productResults.get(0).get("units_sold")).intValue(),
                productResults.isEmpty() ? 0.0 : ((Number) productResults.get(0).get("revenue")).doubleValue(),
                ((Number) summary.get("unique_customers")).intValue(),
                start + " to " + end
            );
        } catch (Exception e) {
            log.error("Error getting sales summary", e);
            return new SalesSummaryDTO();
        }
    }

    /**
     * Get best-selling products ranked by units sold or revenue within a date range (top 10)
     */
    @Tool(description = "Get top 10 best-selling products by units sold within a date range. Includes category, revenue, market share, and transaction count")
    public List<ProductSalesDTO> getTopSellingProducts(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
            
            String sql = "SELECT " +
                    "COALESCE(p.name, 'Unknown') as product_name, " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "SUM(bi.quantity) as units_sold, " +
                    "SUM(bi.subtotal) as total_revenue, " +
                    "AVG(bi.subtotal / NULLIF(bi.quantity, 0)) as avg_price, " +
                    "COUNT(DISTINCT b.billing_id) as num_transactions " +
                    "FROM pharmacy.rdp_billing_items bi " +
                    "LEFT JOIN pharmacy.rdp_billings b ON bi.billing_id = b.billing_id " +
                    "LEFT JOIN pharmacy.rdp_products p ON bi.product_id = p.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE DATE(b.billing_date) >= ? AND DATE(b.billing_date) <= ? " +
                    "GROUP BY p.name, pc.name " +
                    "ORDER BY units_sold DESC LIMIT 10 ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, start, end);
            
            // Calculate total sales for market share
            double totalSales = 0;
            for (Map<String, Object> row : results) {
                totalSales += ((Number) row.getOrDefault("total_revenue", 0)).doubleValue();
            }
            
            List<ProductSalesDTO> products = new ArrayList<>();
            for (Map<String, Object> row : results) {
                double revenue = ((Number) row.getOrDefault("total_revenue", 0)).doubleValue();
                double marketShare = totalSales > 0 ? (revenue / totalSales) * 100 : 0;
                
                products.add(new ProductSalesDTO(
                    (String) row.get("product_name"),
                    (String) row.get("category"),
                    ((Number) row.getOrDefault("units_sold", 0)).intValue(),
                    revenue,
                    ((Number) row.getOrDefault("avg_price", 0)).doubleValue(),
                    ((Number) row.getOrDefault("num_transactions", 0)).intValue(),
                    marketShare
                ));
            }
            
            return products;
        } catch (Exception e) {
            log.error("Error getting top-selling products", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get today's total sales, transaction count, and revenue summary
     */
    public Map<String, Object> getTodaysSales() {
        try {
            LocalDate today = LocalDate.now();
            return getSalesSummary(today.toString(), today.toString()).getClass().isInstance(new SalesSummaryDTO()) ? 
                Map.of("todaySalesData", getSalesSummary(today.toString(), today.toString())) : 
                Map.of("error", "Unable to fetch today's sales");
        } catch (Exception e) {
            log.error("Error getting today's sales", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Get monthly sales breakdown for entire year (Jan-Dec)
     * Shows sales per month with trends and quarterly totals
     */
    public Map<String, Object> getMonthlySalesBreakdown() {
        try {
            String sql = "SELECT " +
                    "EXTRACT(MONTH FROM b.billing_date) as month, " +
                    "TO_CHAR(b.billing_date, 'Month') as month_name, " +
                    "COUNT(DISTINCT b.billing_id) as transaction_count, " +
                    "COALESCE(SUM(b.grand_total), 0) as monthly_total, " +
                    "COALESCE(AVG(b.grand_total), 0) as avg_transaction_value, " +
                    "COALESCE(SUM(bi.quantity), 0) as total_items_sold " +
                    "FROM pharmacy.rdp_billings b " +
                    "LEFT JOIN pharmacy.rdp_billing_items bi ON b.billing_id = bi.billing_id " +
                    "WHERE EXTRACT(YEAR FROM b.billing_date) = EXTRACT(YEAR FROM CURRENT_DATE) " +
                    "GROUP BY EXTRACT(MONTH FROM b.billing_date), TO_CHAR(b.billing_date, 'Month') " +
                    "ORDER BY EXTRACT(MONTH FROM b.billing_date)";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> response = new java.util.HashMap<>();
            
            // Calculate quarterly totals
            double q1Total = 0, q2Total = 0, q3Total = 0, q4Total = 0;
            double maxMonthSales = 0;
            double minMonthSales = Double.MAX_VALUE;
            String maxMonth = "";
            String minMonth = "";
            
            for (Map<String, Object> row : results) {
                int month = ((Number) row.get("month")).intValue();
                double sales = ((Number) row.getOrDefault("monthly_total", 0)).doubleValue();
                
                if (sales > maxMonthSales) {
                    maxMonthSales = sales;
                    maxMonth = (String) row.get("month_name");
                }
                
                if (sales < minMonthSales && sales > 0) {
                    minMonthSales = sales;
                    minMonth = (String) row.get("month_name");
                }
                
                if (month <= 3) q1Total += sales;
                else if (month <= 6) q2Total += sales;
                else if (month <= 9) q3Total += sales;
                else q4Total += sales;
            }
            
            response.put("monthly_breakdown", results);
            response.put("q1_total", String.format("%.2f", q1Total));
            response.put("q2_total", String.format("%.2f", q2Total));
            response.put("q3_total", String.format("%.2f", q3Total));
            response.put("q4_total", String.format("%.2f", q4Total));
            response.put("max_sales_month", maxMonth);
            response.put("max_sales_amount", String.format("%.2f", maxMonthSales));
            response.put("min_sales_month", minMonth);
            response.put("min_sales_amount", String.format("%.2f", minMonthSales));
            response.put("yearly_total", String.format("%.2f", q1Total + q2Total + q3Total + q4Total));
            
            log.info("Monthly sales breakdown retrieved: {} months", results.size());
            return response;
            
        } catch (Exception e) {
            log.error("Error getting monthly sales breakdown", e);
            return java.util.Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Get sales for specific month (1-12 or month name)
     */
    public Map<String, Object> getSalesForMonth(String month) {
        try {
            int monthNum = parseMonth(month);
            if (monthNum < 1 || monthNum > 12) {
                return java.util.Collections.singletonMap("error", "Invalid month: " + month);
            }
            
            String sql = "SELECT " +
                    "TO_CHAR(b.billing_date, 'YYYY-MM-DD') as billing_date, " +
                    "b.billing_id, " +
                    "c.name as customer_name, " +
                    "b.grand_total, " +
                    "COUNT(DISTINCT bi.product_id) as item_count " +
                    "FROM pharmacy.rdp_billings b " +
                    "LEFT JOIN pharmacy.rdp_customers c ON b.customer_id = c.customer_id " +
                    "LEFT JOIN pharmacy.rdp_billing_items bi ON b.billing_id = bi.billing_id " +
                    "WHERE EXTRACT(MONTH FROM b.billing_date) = ? " +
                    "AND EXTRACT(YEAR FROM b.billing_date) = EXTRACT(YEAR FROM CURRENT_DATE) " +
                    "GROUP BY b.billing_id, TO_CHAR(b.billing_date, 'YYYY-MM-DD'), c.name, b.grand_total " +
                    "ORDER BY b.billing_date DESC";
            
            List<Map<String, Object>> billings = jdbcTemplate.queryForList(sql, monthNum);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("month", getMonthName(monthNum));
            response.put("month_number", monthNum);
            response.put("transaction_count", billings.size());
            response.put("billings", billings);
            
            double total = billings.stream()
                    .mapToDouble(b -> ((Number) b.getOrDefault("grand_total", 0)).doubleValue())
                    .sum();
            response.put("total_sales", String.format("%.2f", total));
            response.put("avg_transaction", String.format("%.2f", billings.isEmpty() ? 0 : total / billings.size()));
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting sales for month", e);
            return java.util.Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Get best selling product with detailed information
     */
    public Map<String, Object> getBestSellingProduct() {
        try {
            String sql = "SELECT " +
                    "p.product_id, " +
                    "p.name as product_name, " +
                    "SUM(bi.quantity) as total_quantity_sold, " +
                    "COALESCE(SUM(bi.subtotal), 0) as total_sales_value, " +
                    "COUNT(DISTINCT b.billing_id) as times_purchased " +
                    "FROM pharmacy.rdp_billing_items bi " +
                    "LEFT JOIN pharmacy.rdp_products p ON bi.product_id = p.product_id " +
                    "LEFT JOIN pharmacy.rdp_billings b ON bi.billing_id = b.billing_id " +
                    "WHERE EXTRACT(YEAR FROM b.billing_date) = EXTRACT(YEAR FROM CURRENT_DATE) " +
                    "GROUP BY p.product_id, p.name " +
                    "ORDER BY total_sales_value DESC LIMIT 1";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            if (results.isEmpty()) {
                return java.util.Collections.singletonMap("message", "No sales data available");
            }
            
            Map<String, Object> response = new java.util.HashMap<>(results.get(0));
            response.put("rank", 1);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting best selling product", e);
            return java.util.Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Get product sales trend by month - which products sell best in which months
     */
    public Map<String, Object> getProductSalesTrendByMonth() {
        try {
            String sql = "SELECT " +
                    "p.name as product_name, " +
                    "EXTRACT(MONTH FROM b.billing_date) as month, " +
                    "TO_CHAR(b.billing_date, 'Month') as month_name, " +
                    "SUM(bi.quantity) as quantity_sold, " +
                    "COALESCE(SUM(bi.subtotal), 0) as sales_value " +
                    "FROM pharmacy.rdp_billing_items bi " +
                    "LEFT JOIN pharmacy.rdp_products p ON bi.product_id = p.product_id " +
                    "LEFT JOIN pharmacy.rdp_billings b ON bi.billing_id = b.billing_id " +
                    "WHERE EXTRACT(YEAR FROM b.billing_date) = EXTRACT(YEAR FROM CURRENT_DATE) " +
                    "GROUP BY p.name, EXTRACT(MONTH FROM b.billing_date), TO_CHAR(b.billing_date, 'Month') " +
                    "ORDER BY p.name, EXTRACT(MONTH FROM b.billing_date)";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("product_monthly_trend", results);
            response.put("record_count", results.size());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting product sales trend", e);
            return java.util.Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Get quarterly sales breakdown (Q1, Q2, Q3, Q4)
     */
    public Map<String, Object> getQuarterlySales() {
        try {
            Map<String, Object> monthlyData = getMonthlySalesBreakdown();
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("q1_sales", monthlyData.get("q1_total"));
            response.put("q2_sales", monthlyData.get("q2_total"));
            response.put("q3_sales", monthlyData.get("q3_total"));
            response.put("q4_sales", monthlyData.get("q4_total"));
            response.put("yearly_total", monthlyData.get("yearly_total"));
            response.put("monthly_breakdown", monthlyData.get("monthly_breakdown"));
            
            log.info("Quarterly sales retrieved successfully");
            return response;
            
        } catch (Exception e) {
            log.error("Error getting quarterly sales", e);
            return java.util.Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Get sales data for a specific product by name within a date range
     */
    public Map<String, Object> getProductSalesByName(String productName, String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
            LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
            
            // First, find the product by name (case-insensitive)
            String productSearchSql = "SELECT product_id, name FROM pharmacy.rdp_products " +
                    "WHERE name ILIKE ? LIMIT 1";
            List<Map<String, Object>> productResults = jdbcTemplate.queryForList(productSearchSql, "%" + productName + "%");
            
            if (productResults.isEmpty()) {
                return java.util.Collections.singletonMap("error", "Product '" + productName + "' not found");
            }
            
            long productId = ((Number) productResults.get(0).get("product_id")).longValue();
            String actualProductName = (String) productResults.get(0).get("name");
            
            // Get sales data for the product
            String salesSql = "SELECT " +
                    "? as product_name, " +
                    "SUM(bi.quantity) as total_units_sold, " +
                    "SUM(bi.subtotal) as total_revenue, " +
                    "AVG(bi.unit_price) as avg_unit_price, " +
                    "MIN(bi.unit_price) as min_unit_price, " +
                    "MAX(bi.unit_price) as max_unit_price, " +
                    "COUNT(DISTINCT bi.billing_id) as total_transactions, " +
                    "COUNT(DISTINCT b.customer_id) as unique_customers " +
                    "FROM pharmacy.rdp_billing_items bi " +
                    "JOIN pharmacy.rdp_billings b ON bi.billing_id = b.billing_id " +
                    "WHERE bi.product_id = ? " +
                    "AND DATE(b.billing_date) >= ? AND DATE(b.billing_date) <= ? ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(salesSql, actualProductName, productId, start, end);
            
            if (results.isEmpty() || results.get(0).get("total_units_sold") == null) {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("product_name", actualProductName);
                response.put("product_id", productId);
                response.put("period", start + " to " + end);
                response.put("total_units_sold", 0);
                response.put("total_revenue", 0);
                response.put("message", "No sales data found for this product in the specified period");
                return response;
            }
            
            Map<String, Object> result = results.get(0);
            Map<String, Object> response = new java.util.HashMap<>();
            
            response.put("product_id", productId);
            response.put("product_name", actualProductName);
            response.put("period", start + " to " + end);
            response.put("total_units_sold", result.get("total_units_sold"));
            response.put("total_revenue", String.format("%.2f", ((Number) result.get("total_revenue")).doubleValue()));
            response.put("unit_price", String.format("%.2f", ((Number) result.get("avg_unit_price")).doubleValue()));
            response.put("min_unit_price", String.format("%.2f", ((Number) result.get("min_unit_price")).doubleValue()));
            response.put("max_unit_price", String.format("%.2f", ((Number) result.get("max_unit_price")).doubleValue()));
            response.put("total_transactions", result.get("total_transactions"));
            response.put("unique_customers", result.get("unique_customers"));
            
            if (((Number) result.get("total_units_sold")).intValue() > 0) {
                double avgPricePerUnit = ((Number) result.get("total_revenue")).doubleValue() / 
                                        ((Number) result.get("total_units_sold")).intValue();
                response.put("avg_price_per_unit", String.format("%.2f", avgPricePerUnit));
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting product sales by name", e);
            return java.util.Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Helper: Parse month from string (1-12 or month name)
     */
    private int parseMonth(String month) {
        try {
            return Integer.parseInt(month);
        } catch (NumberFormatException e) {
            String monthLower = month.toLowerCase().trim();
            String[] monthNames = {"january", "february", "march", "april", "may", "june",
                                  "july", "august", "september", "october", "november", "december",
                                  "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
            int[] monthNumbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                                 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
            
            for (int i = 0; i < monthNames.length; i++) {
                if (monthNames[i].equals(monthLower)) {
                    return monthNumbers[i];
                }
            }
            return -1;
        }
    }

    /**
     * Helper: Get month name from number
     */
    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return (month >= 1 && month <= 12) ? months[month - 1] : "Unknown";
    }
}
