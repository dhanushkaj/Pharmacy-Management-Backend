package com.rdp.dto;

import java.math.BigDecimal;

public record BillingReturnRecordResponse(
        Long billingReturnRecordId,
        Long originalBillingItemId,
        String originalBillingNumber,
        Long productId,
        String productCode,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal refundAmount,
        BigDecimal discountPercentage
) {}
