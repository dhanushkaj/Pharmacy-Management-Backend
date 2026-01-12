package com.rdp.dto;

import com.rdp.model.Billing;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record BillingRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotEmpty(message = "At least one billing item is required")
        @Valid
        List<BillingItemRequest> items,

        BigDecimal totalDiscount,

        BigDecimal discountPercentage,

        Billing.PaymentMethod paymentMethod,

        String notes
) {
}
