package com.rdp.dto;

import com.rdp.model.BinType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementDto {
    private Long id;
    private Long productId;
    private String batchNo;
    private BinType fromBin;
    private BinType toBin;
    private Integer quantity;
    private String referenceType;
    private String referenceId;
    private String performedBy;
    private String remarks;
    private OffsetDateTime createdAt;
    private Integer inventoryBalanceAfter;
}
