package com.rdp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountLineDTO {
    private Long id;
    private Long sessionId;
    private Long productId;
    private Long inventoryItemId;
    private String productCode;
    private String productName;
    private BigDecimal sellPrice;
    private Integer systemQtyAtCount;
    private Integer physicalQty;
    private Integer variance;
    private Boolean counted;
    private String lineComment;
    private LocalDateTime createdAt;
}
