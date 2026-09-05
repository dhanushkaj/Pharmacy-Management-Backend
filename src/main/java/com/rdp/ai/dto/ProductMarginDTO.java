package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for pricing and margin queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMarginDTO {
    private String productName;
    private String sku;
    private String category;
    private Double costPrice;
    private Double sellingPrice;
    private Double marginAmount;
    private Double marginPercent;
    private Double markupPercent;
    private String priceStatus; // PREMIUM, STANDARD, BUDGET, LOW_MARGIN
}
