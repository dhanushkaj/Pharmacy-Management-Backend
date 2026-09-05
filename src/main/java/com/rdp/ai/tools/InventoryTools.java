package com.rdp.ai.tools;

import com.rdp.ai.dto.ProductStockDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI Tools for inventory and stock management queries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryTools {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get list of products that are low in stock or below reorder level
     * Useful for inventory management and reorder decisions.
     */
    @Tool(description = "Get list of products that are low in stock (below or near reorder level). Returns product name, current stock, reorder level, category, stock status, and estimated inventory value")
    public List<ProductStockDTO> getLowStockItems() {
        try {
            String sql = "SELECT " +
                    "p.name as product_name, " +
                    "p.sku as sku, " +
                    "COALESCE(i.stock, 0) as current_stock, " +
                    "COALESCE(p.min_stock, 10) as reorder_level, " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "COALESCE(i.stock, 0) * COALESCE(p.cost_price, 0) as estimated_value, " +
                    "CASE WHEN COALESCE(i.stock, 0) = 0 THEN 'OUT_OF_STOCK' " +
                    "     WHEN COALESCE(i.stock, 0) <= COALESCE(p.min_stock, 10) THEN 'LOW_STOCK' " +
                    "     WHEN COALESCE(i.stock, 0) <= COALESCE(p.min_stock, 10) * 2 THEN 'MEDIUM' " +
                    "     ELSE 'OK' END as status " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_inventory_items i ON p.product_id = i.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE COALESCE(i.stock, 0) <= COALESCE(p.min_stock, 10) * 1.5 " +
                    "ORDER BY i.stock ASC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            List<ProductStockDTO> items = new ArrayList<>();
            
            for (Map<String, Object> row : results) {
                items.add(new ProductStockDTO(
                    (String) row.get("product_name"),
                    (String) row.get("sku"),
                    ((Number) row.getOrDefault("current_stock", 0)).intValue(),
                    ((Number) row.getOrDefault("reorder_level", 0)).intValue(),
                    (String) row.get("category"),
                    (String) row.get("status"),
                    row.get("estimated_value") != null ? ((Number) row.get("estimated_value")).doubleValue() : 0.0
                ));
            }
            
            return items;
        } catch (Exception e) {
            log.error("Error getting low stock items", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get stock status for a specific product by name or SKU
     */
    @Tool(description = "Get stock status for a specific product by name or SKU. Returns current stock level, category, reorder level, status (OUT_OF_STOCK, LOW_STOCK, MEDIUM, OK), and estimated value")
    public ProductStockDTO getProductStock(String productNameOrSku) {
        try {
            String sql = "SELECT " +
                    "p.name as product_name, " +
                    "p.sku as sku, " +
                    "COALESCE(i.stock, 0) as current_stock, " +
                    "COALESCE(p.min_stock, 10) as reorder_level, " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "COALESCE(i.stock, 0) * COALESCE(p.cost_price, 0) as estimated_value, " +
                    "CASE WHEN COALESCE(i.stock, 0) = 0 THEN 'OUT_OF_STOCK' " +
                    "     WHEN COALESCE(i.stock, 0) <= COALESCE(p.min_stock, 10) THEN 'LOW_STOCK' " +
                    "     WHEN COALESCE(i.stock, 0) <= COALESCE(p.min_stock, 10) * 2 THEN 'MEDIUM' " +
                    "     ELSE 'OK' END as status " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_inventory_items i ON p.product_id = i.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE LOWER(p.name) LIKE ? OR LOWER(p.sku) LIKE ? " +
                    "LIMIT 1";
            
            String searchTerm = "%" + productNameOrSku.toLowerCase() + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, searchTerm, searchTerm);
            
            if (!results.isEmpty()) {
                Map<String, Object> row = results.get(0);
                return new ProductStockDTO(
                    (String) row.get("product_name"),
                    (String) row.get("sku"),
                    ((Number) row.getOrDefault("current_stock", 0)).intValue(),
                    ((Number) row.getOrDefault("reorder_level", 0)).intValue(),
                    (String) row.get("category"),
                    (String) row.get("status"),
                    row.get("estimated_value") != null ? ((Number) row.get("estimated_value")).doubleValue() : 0.0
                );
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error getting product stock", e);
            return null;
        }
    }

    /**
     * Get the total estimated value of current inventory by category
     */
    @Tool(description = "Get total inventory value breakdown by category. Returns product count per category, total units, and total value per category")
    public Map<String, Object> getTotalInventoryValue() {
        try {
            String sql = "SELECT " +
                    "COALESCE(pc.name, 'Uncategorized') as category, " +
                    "COUNT(DISTINCT p.product_id) as product_count, " +
                    "SUM(COALESCE(i.stock, 0)) as total_units, " +
                    "SUM(COALESCE(i.stock, 0) * COALESCE(p.cost_price, 0)) as total_value " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_inventory_items i ON p.product_id = i.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "GROUP BY COALESCE(pc.name, 'Uncategorized') " +
                    "ORDER BY total_value DESC ";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            double totalValue = 0;
            int totalProducts = 0;
            for (Map<String, Object> row : results) {
                totalValue += ((Number) row.getOrDefault("total_value", 0)).doubleValue();
                totalProducts += ((Number) row.getOrDefault("product_count", 0)).intValue();
            }
            
            return Map.of(
                "totalInventoryValue", totalValue,
                "totalProductsTracked", totalProducts,
                "categoryBreakdown", results
            );
        } catch (Exception e) {
            log.error("Error getting total inventory value", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Get inventory items by category name with stock filtering
     * Returns all products in the category that have stock greater than minStock threshold
     * Includes stock quantity and selling prices from rdp_inventory_items
     */
    @Tool(description = "Get all inventory items for a category with stock greater than minimum threshold. Returns product name, SKU, stock quantity, selling price, cost price, batch number, and total inventory value")
    public Map<String, Object> getInventoryByCategory(String categoryName, int minStock) {
        try {
            if (minStock < 0) minStock = 1;
            
            String sql = "SELECT " +
                    "pc.name as category_name, " +
                    "p.product_id, " +
                    "p.name as product_name, " +
                    "p.sku, " +
                    "COALESCE(i.stock, 0) as current_stock, " +
                    "COALESCE(i.price, p.selling_price) as selling_price, " +
                    "COALESCE(p.cost_price, 0) as cost_price, " +
                    "COALESCE(i.stock, 0) * COALESCE(i.price, p.selling_price) as total_value, " +
                    "COALESCE(i.batch_no, 'N/A') as batch_no, " +
                    "COALESCE(p.min_stock, 10) as reorder_level " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_inventory_items i ON p.product_id = i.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE LOWER(pc.name) LIKE ? " +
                    "AND COALESCE(i.stock, 0) > ? " +
                    "ORDER BY i.stock DESC ";
            
            String searchTerm = "%" + categoryName.toLowerCase() + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, searchTerm, minStock);
            
            log.info("Found {} inventory items in category '{}' with stock > {}", results.size(), categoryName, minStock);
            
            // Calculate summary stats
            double totalCategoryValue = 0;
            int totalUnits = 0;
            int productCount = results.size();
            
            for (Map<String, Object> row : results) {
                totalUnits += ((Number) row.getOrDefault("current_stock", 0)).intValue();
                Object value = row.get("total_value");
                if (value != null) {
                    totalCategoryValue += ((Number) value).doubleValue();
                }
            }
            
            return Map.of(
                "categoryName", categoryName,
                "minStockThreshold", minStock,
                "productCount", productCount,
                "totalUnits", totalUnits,
                "totalCategoryValue", totalCategoryValue,
                "inventoryItems", results
            );
        } catch (Exception e) {
            log.error("Error getting inventory by category: {}", categoryName, e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Get inventory by category with custom min/max stock range
     * Returns products within the specified stock range
     */
    @Tool(description = "Get inventory items for a category within a specific stock range (minStock to maxStock). Useful for filtering inventory by quantity range")
    public Map<String, Object> getInventoryByCategoryWithRange(String categoryName, int minStock, int maxStock) {
        try {
            if (minStock < 0) minStock = 1;
            if (maxStock <= minStock) maxStock = Integer.MAX_VALUE;
            
            String sql = "SELECT " +
                    "pc.name as category_name, " +
                    "p.product_id, " +
                    "p.name as product_name, " +
                    "p.sku, " +
                    "COALESCE(i.stock, 0) as current_stock, " +
                    "COALESCE(i.price, p.selling_price) as selling_price, " +
                    "COALESCE(p.cost_price, 0) as cost_price, " +
                    "COALESCE(i.stock, 0) * COALESCE(i.price, p.selling_price) as total_value, " +
                    "COALESCE(i.batch_no, 'N/A') as batch_no, " +
                    "COALESCE(p.min_stock, 10) as reorder_level " +
                    "FROM pharmacy.rdp_products p " +
                    "LEFT JOIN pharmacy.rdp_inventory_items i ON p.product_id = i.product_id " +
                    "LEFT JOIN pharmacy.rdp_categories pc ON p.category_id = pc.category_id " +
                    "WHERE LOWER(pc.name) LIKE ? " +
                    "AND COALESCE(i.stock, 0) >= ? " +
                    "AND COALESCE(i.stock, 0) <= ? " +
                    "ORDER BY i.stock DESC ";
            
            String searchTerm = "%" + categoryName.toLowerCase() + "%";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, searchTerm, minStock, maxStock);
            
            log.info("Found {} inventory items in category '{}' with stock between {} and {}", 
                    results.size(), categoryName, minStock, maxStock);
            
            // Calculate summary stats
            double totalCategoryValue = 0;
            int totalUnits = 0;
            int productCount = results.size();
            
            for (Map<String, Object> row : results) {
                totalUnits += ((Number) row.getOrDefault("current_stock", 0)).intValue();
                Object value = row.get("total_value");
                if (value != null) {
                    totalCategoryValue += ((Number) value).doubleValue();
                }
            }
            
            return Map.of(
                "categoryName", categoryName,
                "minStockThreshold", minStock,
                "maxStockThreshold", maxStock,
                "productCount", productCount,
                "totalUnits", totalUnits,
                "totalCategoryValue", totalCategoryValue,
                "inventoryItems", results
            );
        } catch (Exception e) {
            log.error("Error getting inventory by category with range: {}", categoryName, e);
            return Map.of("error", e.getMessage());
        }
    }
}
