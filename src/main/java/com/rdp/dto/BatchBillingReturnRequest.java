package com.rdp.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BatchBillingReturnRequest(
        @NotEmpty(message = "At least one return item is required")
        List<ReturnItemRequest> returnItems,

        Long newBillingId  // Optional: If provided, link return records to this new billing
) {
    public record ReturnItemRequest(
            @NotNull(message = "Billing item ID is required")
            Long billingItemId,

            @NotNull(message = "Return quantity is required")
            Integer returnQty
    ) {}
}
