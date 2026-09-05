package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for inventory/stock queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockDTO {
    private String productName;
    private String sku;
    private Integer currentStock;
    private Integer reorderLevel;
    private String category;
    private String status; // LOW_STOCK, MEDIUM, OK, OUT_OF_STOCK
    private Double estimatedValue;
}
