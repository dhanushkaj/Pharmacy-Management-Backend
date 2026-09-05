package com.rdp.ai.tools;

import com.rdp.ai.dto.ProductMarginDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI Tools for pricing and margin analysis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PricingTools {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get detailed pricing and profit margin information for a specific product by name or SKU.
     * Shows cost price, selling price, margin percentage, and markup.
     */
    public ProductMarginDTO getProductMargin(String productNameOrSku) {
        try {
            String sql = "SELECT " +
                    "p.name as product_name, " +
                    "p.sku, " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "COALESCE(p.cost_price, 0) as cost_price, " +
                    "COALESCE(p.selling_price, 0) as selling_price " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE LOWER(p.name) LIKE ? OR LOWER(p.sku) LIKE ? " +
                    "LIMIT 1 ";
            
            String searchTerm = "%" + productNameOrSku.toLowerCase() + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, searchTerm, searchTerm);
            
            if (!results.isEmpty()) {
                Map<String, Object> row = results.get(0);
                double costPrice = ((Number) row.getOrDefault("cost_price", 0)).doubleValue();
                double sellingPrice = ((Number) row.getOrDefault("selling_price", 0)).doubleValue();
                
                double marginAmount = sellingPrice - costPrice;
                double marginPercent = costPrice > 0 ? (marginAmount / sellingPrice) * 100 : 0;
                double markupPercent = costPrice > 0 ? (marginAmount / costPrice) * 100 : 0;
                
                String priceStatus = "STANDARD";
                if (marginPercent < 10) priceStatus = "LOW_MARGIN";
                else if (marginPercent > 50) priceStatus = "PREMIUM";
                else if (marginPercent < 20) priceStatus = "BUDGET";
                
                return new ProductMarginDTO(
                    (String) row.get("product_name"),
                    (String) row.get("sku"),
                    (String) row.get("category"),
                    costPrice,
                    sellingPrice,
                    marginAmount,
                    marginPercent,
                    markupPercent,
                    priceStatus
                );
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error getting product margin", e);
            return null;
        }
    }

    /**
     * Get all products with profit margins below a threshold (default 15%).
     * Useful for identifying products that may need price review or promotional strategy.
     */
    public List<ProductMarginDTO> getLowMarginProducts(double marginThreshold) {
        try {
            if (marginThreshold <= 0 || marginThreshold > 50) marginThreshold = 15;
            
            String sql = "SELECT " +
                    "p.name as product_name, " +
                    "p.sku, " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "COALESCE(p.cost_price, 0) as cost_price, " +
                    "COALESCE(p.selling_price, 0) as selling_price " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE COALESCE(p.selling_price, 0) > 0 " +
                    "AND (COALESCE(p.selling_price, 0) - COALESCE(p.cost_price, 0)) / COALESCE(p.selling_price, 1) * 100 < ? " +
                    "ORDER BY (COALESCE(p.selling_price, 0) - COALESCE(p.cost_price, 0)) / COALESCE(p.selling_price, 1) * 100 ASC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, marginThreshold);
            List<ProductMarginDTO> products = new ArrayList<>();
            
            for (Map<String, Object> row : results) {
                double costPrice = ((Number) row.getOrDefault("cost_price", 0)).doubleValue();
                double sellingPrice = ((Number) row.getOrDefault("selling_price", 0)).doubleValue();
                
                double marginAmount = sellingPrice - costPrice;
                double marginPercent = sellingPrice > 0 ? (marginAmount / sellingPrice) * 100 : 0;
                double markupPercent = costPrice > 0 ? (marginAmount / costPrice) * 100 : 0;
                
                products.add(new ProductMarginDTO(
                    (String) row.get("product_name"),
                    (String) row.get("sku"),
                    (String) row.get("category"),
                    costPrice,
                    sellingPrice,
                    marginAmount,
                    marginPercent,
                    markupPercent,
                    "LOW_MARGIN"
                ));
            }
            
            return products;
        } catch (Exception e) {
            log.error("Error getting low margin products", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get average profit margins by product category to identify which categories are most profitable
     */
    public List<Map<String, Object>> getMarginsByCategory() {
        try {
            String sql = "SELECT " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "COUNT(DISTINCT p.product_id) as product_count, " +
                    "AVG((COALESCE(p.selling_price, 0) - COALESCE(p.cost_price, 0)) / NULLIF(COALESCE(p.selling_price, 1), 0) * 100) as avg_margin_percent, " +
                    "AVG(COALESCE(p.selling_price, 0)) as avg_selling_price, " +
                    "AVG(COALESCE(p.cost_price, 0)) as avg_cost_price " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "GROUP BY COALESCE(pc.name, 'Uncategorized') " +
                    "ORDER BY avg_margin_percent DESC ";
            
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Error getting margins by category", e);
            return new ArrayList<>();
        }
    }
}
