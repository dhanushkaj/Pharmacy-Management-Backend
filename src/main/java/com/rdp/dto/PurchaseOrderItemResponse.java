package com.rdp.dto;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Long itemId,
        Long productId,
        String productName,
        String productCode,
        Integer quantity,
        BigDecimal unitCost
) {}