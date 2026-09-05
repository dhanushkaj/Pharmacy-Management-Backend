package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for top-selling products queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesDTO {
    private String productName;
    private String category;
    private Integer unitsSold;
    private Double totalRevenue;
    private Double averagePrice;
    private Integer numberOfTransactions;
    private Double marketShare; // percentage of total sales
}
