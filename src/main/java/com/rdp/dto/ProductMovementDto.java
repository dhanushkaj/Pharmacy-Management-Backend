package com.rdp.dto;

import java.util.List;

public record ProductMovementDto(
    Long productId,
    String productCode,
    String name,
    String categoryName,
    List<Integer> monthlyQuantities
) {}
