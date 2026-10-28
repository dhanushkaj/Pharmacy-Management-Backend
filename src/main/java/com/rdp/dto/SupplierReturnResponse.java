package com.rdp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SupplierReturnResponse(
        Long returnId,
        String returnNumber,
        LocalDateTime returnDate,
        Long supplierId,
        String supplierName,
        BigDecimal totalReturnAmount,
        List<SupplierReturnItemResponse> returnItems,
        String notes
) { }
