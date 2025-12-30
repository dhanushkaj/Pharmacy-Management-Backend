package com.rdp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySummaryDto {
    private Long productId;
    private String batchNo;
    private LocalDate expiryDate;
    private Integer inventoryQuantity;
    private Boolean nearExpiry;
}
