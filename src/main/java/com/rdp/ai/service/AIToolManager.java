package com.rdp.ai.service;

import com.rdp.ai.dto.ProductStockDTO;
import com.rdp.ai.dto.ProductSalesDTO;
import com.rdp.ai.dto.SalesSummaryDTO;
import com.rdp.ai.dto.CustomerSpendDTO;
import com.rdp.ai.dto.ProductMarginDTO;
import com.rdp.ai.tools.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Tool Manager - Routes queries to appropriate tool methods
 * Provides intelligent query routing for pharmacy business intelligence
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIToolManager {

    private final InventoryTools inventoryTools;
    private final SalesTools salesTools;
    private final CustomerTools customerTools;
    private final PricingTools pricingTools;

    /**
     * Detect what type of query this is and route to appropriate tools
     */
    public Map<String, Object> routeQuery(String query) {
        String queryLower = query.toLowerCase();
        Map<String, Object> results = new HashMap<>();

        // Inventory queries - "low", "worst", "running out", "out of stock"
        if (containsKeywords(queryLower, "stock", "inventory", "low", "worst", "reorder", "out of stock", "running out")) {
            results.put("inventory", routeInventoryQuery(queryLower));
        }

        // Sales queries - "top", "best", "sold", "selling"
        if (containsKeywords(queryLower, "sales", "revenue", "sold", "selling", "top", "best","range","duration")) {
            results.put("sales", routeSalesQuery(queryLower));
        }

        // Customer queries - "customer", "client", "buyer", "top", "best", "loyal", "good", "worst", "not loyal"
        if (containsKeywords(queryLower, "customer", "client", "buyer", "purchase", "spend", "loyal", "good", "worst", "top", "best")) {
            results.put("customer", routeCustomerQuery(queryLower));
        }

        // Pricing queries - "price", "margin", "cost", "profit", "markup", "low", "worst"
        if (containsKeywords(queryLower, "price", "margin", "cost", "profit", "markup", "low", "worst")) {
            results.put("pricing", routePricingQuery(queryLower));
        }

        return results;
    }

    /**
     * Route inventory-specific queries
     */
    private Map<String, Object> routeInventoryQuery(String queryLower) {
        try {
            // Category-based inventory query - "give me inventory for category XYZ with stock > 1"
            // or "show me stock for category XYZ" or "category XYZ items"
            if (queryLower.contains("category")) {
                String category = extractCategoryName(queryLower);
                if (category != null && !category.isEmpty()) {
                    // Check if user specified a range or just default > 1
                    int minStock = 1;
                    if (queryLower.contains("range")) {
                        // Try to extract numeric range from query
                        int rangeIdx = queryLower.indexOf("range");
                        String afterRange = queryLower.substring(rangeIdx);
                        // Simple extraction: look for numbers like "range 5 to 20" or "range > 10"
                        if (afterRange.contains(">")) {
                            String numPart = afterRange.replaceAll("[^0-9]", "").substring(0, Math.min(3, afterRange.length()));
                            if (!numPart.isEmpty()) {
                                try {
                                    minStock = Integer.parseInt(numPart);
                                } catch (NumberFormatException e) {
                                    log.debug("Could not parse min stock from range");
                                }
                            }
                        }
                    }
                    return Map.of("categoryInventory", inventoryTools.getInventoryByCategory(category, minStock));
                }
            }
            
            // Low/worst/running out → low stock items
            if (queryLower.contains("low") || queryLower.contains("worst") || queryLower.contains("reorder") 
                || queryLower.contains("running out") || queryLower.contains("out of stock") 
                || queryLower.contains("stock level")) {
                return Map.of("lowStockItems", inventoryTools.getLowStockItems());
            } 
            // Value/total/how much → inventory value
            else if (queryLower.contains("value") || queryLower.contains("total") || queryLower.contains("how much")
                       || queryLower.contains("worth")) {
                return inventoryTools.getTotalInventoryValue();
            }
        } catch (Exception e) {
            log.error("Error routing inventory query", e);
        }
        return Map.of();
    }

    /**
     * Extract category name from query string
     * Looks for patterns like "category medicine", "category vitamin", etc.
     */
    private String extractCategoryName(String queryLower) {
        // Common pharmacy categories
        String[] categories = {"medicine", "vitamin", "supplement", "syrup", "injection", "tablet", "cream", "ointment", "lotion", "powder"};
        
        for (String category : categories) {
            if (queryLower.contains(category)) {
                // Extract the full category name if followed by descriptive words
                int idx = queryLower.indexOf(category);
                String categoryName = category;
                
                // Try to get more context after the category keyword
                int endIdx = idx + category.length();
                if (endIdx < queryLower.length()) {
                    String remaining = queryLower.substring(endIdx);
                    // Look for the next space or "with", "stock", "inventory", "show", etc.
                    int nextBoundary = remaining.indexOf(" with ");
                    if (nextBoundary == -1) nextBoundary = remaining.indexOf(" stock");
                    if (nextBoundary == -1) nextBoundary = remaining.indexOf(" inventory");
                    if (nextBoundary == -1) nextBoundary = remaining.indexOf(" show");
                    if (nextBoundary == -1) nextBoundary = remaining.indexOf(" have");
                    
                    if (nextBoundary > 0) {
                        String extra = remaining.substring(0, nextBoundary).trim();
                        if (!extra.isEmpty() && !extra.matches(".*[0-9]+.*")) {
                            categoryName = categoryName + " " + extra;
                        }
                    }
                }
                
                log.info("Extracted category from query: {}", categoryName);
                return categoryName;
            }
        }
        
        // If no predefined category found, try to extract text after "category" keyword
        if (queryLower.contains("category")) {
            int idx = queryLower.indexOf("category");
            int startIdx = idx + "category".length();
            if (startIdx < queryLower.length()) {
                String remaining = queryLower.substring(startIdx).trim();
                // Get the first word or two after "category"
                String[] words = remaining.split(" ");
                if (words.length > 0) {
                    // Take first 1-2 words as category name, stop at special keywords
                    StringBuilder categoryName = new StringBuilder(words[0]);
                    if (words.length > 1 && !isSpecialKeyword(words[1])) {
                        categoryName.append(" ").append(words[1]);
                    }
                    log.info("Extracted generic category from query: {}", categoryName);
                    return categoryName.toString();
                }
            }
        }
        
        return null;
    }

    /**
     * Check if a word is a special keyword that shouldn't be included in category name
     */
    private boolean isSpecialKeyword(String word) {
        String[] keywords = {"with", "stock", "inventory", "show", "have", "items", "products", "range", "greater", "than", "more", "less"};
        for (String kw : keywords) {
            if (word.equals(kw) || word.startsWith(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Route sales-specific queries
     */
    private Map<String, Object> routeSalesQuery(String queryLower) {
        try {
            // Product-specific sales query - "FINAST 5MG sales", "Glucophage sales", "Aspirin sales", etc.
            // Detect if query is asking about a specific product's sales metrics
            if (queryLower.contains("sales") && !queryLower.contains("monthly") && !queryLower.contains("monthly") 
                && !queryLower.contains("quarter") && !queryLower.contains("trend") && !queryLower.contains("best")
                && !queryLower.contains("top") && !queryLower.contains("by category")) {
                
                // Try to extract product name from query
                String productName = extractProductNameFromQuery(queryLower);
                if (productName != null && !productName.isEmpty()) {
                    // Get current month (August 2026)
                    java.time.LocalDate today = java.time.LocalDate.now();
                    String startDate = today.getYear() + "-" + String.format("%02d", today.getMonthValue()) + "-01";
                    String endDate = today.toString();
                    
                    log.info("Routing to product-specific sales query for product: {}", productName);
                    return Map.of("product_sales", salesTools.getProductSalesByName(productName, startDate, endDate));
                }
            }
            
            // Monthly sales breakdown query
            if (queryLower.contains("monthly") || queryLower.contains("month") || queryLower.contains("jan") 
                || queryLower.contains("feb") || queryLower.contains("mar") || queryLower.contains("apr") 
                || queryLower.contains("may") || queryLower.contains("jun") || queryLower.contains("july") 
                || queryLower.contains("august") || queryLower.contains("sep") || queryLower.contains("oct") 
                || queryLower.contains("nov") || queryLower.contains("dec") || queryLower.contains("january")
                || queryLower.contains("february") || queryLower.contains("march") || queryLower.contains("april")
                || queryLower.contains("june") || queryLower.contains("september") || queryLower.contains("october")
                || queryLower.contains("november") || queryLower.contains("december")) {
                
                // Extract specific month if mentioned
                String[] monthNames = {"january", "february", "march", "april", "may", "june",
                                      "july", "august", "september", "october", "november", "december",
                                      "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
                
                for (String m : monthNames) {
                    if (queryLower.contains(m)) {
                        return Map.of("monthly_sales", salesTools.getSalesForMonth(m));
                    }
                }
                
                // No specific month, return full breakdown
                return Map.of("monthly_breakdown", salesTools.getMonthlySalesBreakdown());
            }
            
            // Quarterly sales query
            else if (queryLower.contains("quarterly") || queryLower.contains("quarter") 
                     || queryLower.contains("q1") || queryLower.contains("q2") || queryLower.contains("q3") 
                     || queryLower.contains("q4")) {
                return Map.of("quarterly_sales", salesTools.getQuarterlySales());
            }
            
            // Best selling product query
            else if (queryLower.contains("best selling") || queryLower.contains("top selling") 
                     || queryLower.contains("best product") || queryLower.contains("top product")) {
                return Map.of("best_product", salesTools.getBestSellingProduct(),
                             "top_products", salesTools.getTopSellingProducts("2026-01-01", "2026-12-31"));
            }
            
            // Product trend query
            else if (queryLower.contains("trend") || queryLower.contains("sale trend") 
                     || queryLower.contains("product trend") || queryLower.contains("which product")) {
                return Map.of("product_trend", salesTools.getProductSalesTrendByMonth());
            }
            
            // Sales comparison query (two months)
            else if (queryLower.contains("compare") || queryLower.contains("vs") || queryLower.contains("versus")) {
                // This would need more sophisticated parsing to extract two months
                return Map.of("monthly_breakdown", salesTools.getMonthlySalesBreakdown());
            }
            
            // Default: show monthly breakdown
            else if (queryLower.contains("sales") || queryLower.contains("revenue") 
                     || queryLower.contains("selling") || queryLower.contains("graph") 
                     || queryLower.contains("chart")) {
                return Map.of("monthly_breakdown", salesTools.getMonthlySalesBreakdown());
            }
        } catch (Exception e) {
            log.error("Error routing sales query", e);
        }
        return Map.of();
    }

    /**
     * Route customer-specific queries
     */
    private Map<String, Object> routeCustomerQuery(String queryLower) {
        try {
            // Top/best/loyal/good customers
            if (queryLower.contains("top") || queryLower.contains("best") || queryLower.contains("loyal") 
                || queryLower.contains("good") || queryLower.contains("highest") || queryLower.contains("biggest") 
                || queryLower.contains("main") || queryLower.contains("high value")) {
                return Map.of("topCustomers", customerTools.getTopCustomers(10));
            } 
            // Worst/low/not loyal/inactive customers
            else if (queryLower.contains("worst") || queryLower.contains("low") || queryLower.contains("not loyal")
                     || queryLower.contains("inactive") || queryLower.contains("not visited") 
                     || queryLower.contains("haven't") || queryLower.contains("at risk") 
                     || queryLower.contains("not purchased")) {
                return Map.of("inactiveCustomers", customerTools.getInactiveCustomers(60));
            }
        } catch (Exception e) {
            log.error("Error routing customer query", e);
        }
        return Map.of();
    }

    /**
     * Route pricing-specific queries
     */
    private Map<String, Object> routePricingQuery(String queryLower) {
        try {
            // Low/worst margin products
            if (queryLower.contains("low") || queryLower.contains("worst") || queryLower.contains("margin") 
                || queryLower.contains("thin") || queryLower.contains("poor profit")) {
                return Map.of("lowMarginProducts", pricingTools.getLowMarginProducts(15));
            } 
            // Margins by category
            else if (queryLower.contains("category") || queryLower.contains("by category")
                       || queryLower.contains("compare")) {
                return Map.of("marginsByCategory", pricingTools.getMarginsByCategory());
            }
        } catch (Exception e) {
            log.error("Error routing pricing query", e);
        }
        return Map.of();
    }

    /**
     * Check if query contains any of the keywords
     */
    private boolean containsKeywords(String query, String... keywords) {
        for (String keyword : keywords) {
            if (query.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get detailed information about a specific product
     */
    public Map<String, Object> getProductDetail(String productName) {
        Map<String, Object> details = new HashMap<>();
        
        ProductStockDTO stock = inventoryTools.getProductStock(productName);
        if (stock != null) {
            details.put("stock", stock);
        }
        
        ProductMarginDTO margin = pricingTools.getProductMargin(productName);
        if (margin != null) {
            details.put("margin", margin);
        }
        
        return details;
    }

    /**
     * Get comprehensive customer analysis
     */
    public Map<String, Object> getCustomerAnalysis() {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("topCustomers", customerTools.getTopCustomers(10));
        analysis.put("inactiveCustomers", customerTools.getInactiveCustomers(60));
        return analysis;
    }

    /**
     * Get comprehensive sales analysis
     */
    public Map<String, Object> getSalesAnalysis(String startDate, String endDate) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("summary", salesTools.getSalesSummary(startDate, endDate));
        analysis.put("topProducts", salesTools.getTopSellingProducts(startDate, endDate));
        return analysis;
    }

    /**
     * Extract product name from user query
     * Examples: "Glucophage sales", "FINAST 5MG sales", "Aspirin sales this month"
     */
    private String extractProductNameFromQuery(String queryLower) {
        // Remove common sales-related keywords to isolate product name
        String query = queryLower
            .replace("sales", "")
            .replace("sale", "")
            .replace("sold", "")
            .replace("selling", "")
            .replace("revenue", "")
            .replace("how much", "")
            .replace("in august", "")
            .replace("in july", "")
            .replace("in june", "")
            .replace("in may", "")
            .replace("in april", "")
            .replace("in march", "")
            .replace("in february", "")
            .replace("in january", "")
            .replace("in september", "")
            .replace("in october", "")
            .replace("in november", "")
            .replace("in december", "")
            .replace("this month", "")
            .replace("last month", "")
            .replace("this year", "")
            .replace("are there", "")
            .replace("can you", "")
            .replace("check", "")
            .replace("what", "")
            .replace("show me", "")
            .replace("give me", "")
            .replace("tell me", "")
            .replace("for", "")
            .trim();
        
        // Get the remaining text and extract product name
        if (!query.isEmpty()) {
            // Take first 1-4 words as product name (e.g., "GLUCOPHAGE XR 500MG")
            String[] words = query.split("\\s+");
            StringBuilder productName = new StringBuilder();
            
            for (int i = 0; i < Math.min(4, words.length); i++) {
                if (!words[i].isEmpty() && !words[i].equals("the")) {
                    if (productName.length() > 0) productName.append(" ");
                    productName.append(words[i]);
                }
            }
            
            String extracted = productName.toString().trim();
            if (!extracted.isEmpty() && extracted.length() > 2) {
                log.debug("Extracted product name from query: {}", extracted);
                return extracted;
            }
        }
        
        return null;
    }
}

