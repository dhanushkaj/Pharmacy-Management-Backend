package com.rdp.dto;

import com.rdp.model.InventoryReturn;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InventoryReturnRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Return type is required")
        InventoryReturn.ReturnType returnType,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
        BigDecimal unitPrice,

        @NotBlank(message = "Reason is required")
        String reason,

        String batchNo,

        Long customerId,

        String customerName,

        Long supplierId,

        String notes
) {
}
