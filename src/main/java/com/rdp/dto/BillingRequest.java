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

        @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
        BigDecimal discountPercentage,

        Billing.PaymentMethod paymentMethod,

        String notes
) {
}
