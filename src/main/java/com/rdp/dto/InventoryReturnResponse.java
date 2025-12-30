package com.rdp.dto;

import com.rdp.model.InventoryReturn;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryReturnResponse(
        Long returnId,
        Long productId,
        String productCode,
        String productName,
        InventoryReturn.ReturnType returnType,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String reason,
        String batchNo,
        LocalDateTime returnDate,
        String customerName,
        Long supplierId,
        String supplierName,
        String notes,
        LocalDateTime createdAt,
        String createdBy
) {
}
