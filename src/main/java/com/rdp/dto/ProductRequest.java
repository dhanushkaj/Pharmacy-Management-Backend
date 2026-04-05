package com.rdp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
        String name,
        String genericName,
        Long categoryId,
        Long supplierId,
        String productCode,
        String barcode,
        java.math.BigDecimal costPrice, // optional: used when creating inventory item
        java.math.BigDecimal price,     // optional: used when creating inventory item
        Integer stock,                  // optional: used to create inventory item or increment existing
        Integer minStock,
        Integer maxStock,
        java.math.BigDecimal maxDiscount,
        LocalDate expiryDate,
        String patientInstructions,
        String binLocation,
        String packSize
) { }
