package com.rdp.dto;

import java.math.BigDecimal;

public record ProductReportDto(
    Long productId,
    String productCode,
    String name,
    String categoryName,
    BigDecimal price,
    Integer availableInventory,
    Integer minStock,
    Integer maxStock,
    Boolean outOfStock
) {}