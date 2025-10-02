package com.rdp.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderRequest(
        @NotNull Long supplierId,
        LocalDate neededDate,
        @NotNull List<PurchaseOrderItemRequest> items
) {}