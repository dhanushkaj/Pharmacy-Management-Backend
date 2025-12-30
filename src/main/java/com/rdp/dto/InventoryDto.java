package com.rdp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryDto(
        Long id,
        BigDecimal price,
        BigDecimal costPrice,
        Integer stock,
        String batchNo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) { }