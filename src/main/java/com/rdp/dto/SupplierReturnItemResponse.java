package com.rdp.dto;

import java.math.BigDecimal;

public record SupplierReturnItemResponse(
        Long itemId,
        Long productId,
        String productCode,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal itemTotal,
        String batchNo,
        String notes
) { }
