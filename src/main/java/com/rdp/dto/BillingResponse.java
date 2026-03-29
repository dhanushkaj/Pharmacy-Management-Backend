package com.rdp.dto;

import com.rdp.model.Billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillingResponse(
        Long billingId,
        String billingNumber,
        Long customerId,
        String customerName,
        String customerPhone,
        String customerAddress,
        LocalDateTime billingDate,
        BigDecimal subtotal,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        BigDecimal grandTotal,
        Billing.PaymentMethod paymentMethod,
        String notes,
        Boolean isPrinted,
        BigDecimal amountReceived,
        BigDecimal balanceAmount,
        List<BillingItemResponse> items,
        LocalDateTime createdAt,
        String createdBy
) {
}
