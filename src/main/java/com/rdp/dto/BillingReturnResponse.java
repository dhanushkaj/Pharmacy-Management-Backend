package com.rdp.dto;

import java.math.BigDecimal;

public record BillingReturnResponse(
        Long billingItemId,
        Long productId,
        String productCode,
        String productName,
        Integer returnedQty,
        BigDecimal unitPrice,
        BigDecimal refundAmount,
        BigDecimal discountPercentage,
        BigDecimal discountDeducted,
        String billingNumber
) {}
