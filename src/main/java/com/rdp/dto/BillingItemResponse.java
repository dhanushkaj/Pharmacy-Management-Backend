package com.rdp.dto;

import java.math.BigDecimal;

public record BillingItemResponse(
        Long billingItemId,
        Long productId,
        String productCode,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String batchNo,
        Integer returnedQty
) {
}
