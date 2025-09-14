package com.rdp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
        Long productId,
        String name,
        String genericName,
        Long categoryId,
        String categoryName,
        Long supplierId,
        String supplierName,
        String productCode,
        String barcode,
        BigDecimal costPrice,
        BigDecimal price,
        Integer stock,
        Integer minStock,
        Integer maxStock,
        BigDecimal maxDiscount,
        LocalDate expiryDate,
        String patientInstructions,
        String binLocation
) {}