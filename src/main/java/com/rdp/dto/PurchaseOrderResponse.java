package com.rdp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String orderCode,
        LocalDateTime createdAt,
        LocalDate neededDate,
        Long supplierId,
        String supplierName,
        BigDecimal totalCost,
        List<PurchaseOrderItemResponse> items
) {}