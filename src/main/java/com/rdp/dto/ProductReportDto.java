package com.rdp.dto;

public record ProductReportDto(
    Long productId,
    String productCode,
    String name,
    String categoryName,
    Integer availableInventory,
    Integer minStock,
    Integer maxStock,
    Boolean outOfStock
) {}