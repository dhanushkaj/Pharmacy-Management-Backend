package com.rdp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryAuditDetailDto(
    Long auditDetailId,
    Long productId,
    String productName,
    String productCode,
    Integer systemQtyAtExport,
    Integer physicalQty,
    BigDecimal costPrice,
    BigDecimal sellPrice,
    Integer variance,
    String notes
) {}
