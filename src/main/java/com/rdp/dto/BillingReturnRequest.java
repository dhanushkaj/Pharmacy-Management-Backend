package com.rdp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BillingReturnRequest(
        @NotNull(message = "Billing item ID is required")
        Long billingItemId,

        @NotNull(message = "Return quantity is required")
        @Min(value = 1, message = "Return quantity must be at least 1")
        Integer returnQty
) {}
