package com.rdp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SupplierReturnItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        Integer quantity,
        
        @NotNull(message = "Unit price is required")
        @Positive(message = "Unit price must be greater than 0")
        BigDecimal unitPrice,
        
        String batchNo,
        String notes
) { }
