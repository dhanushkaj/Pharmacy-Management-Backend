package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sales summary queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDTO {
    private Double totalRevenue;
    private Long totalTransactions;
    private Double averageTransactionValue;
    private String bestSellingProduct;
    private Integer bestSellingProductUnits;
    private Double bestSellingProductRevenue;
    private Integer uniqueCustomers;
    private String dateRange;
}
