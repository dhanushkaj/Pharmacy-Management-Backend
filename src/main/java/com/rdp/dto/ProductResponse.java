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
        BigDecimal lastPrice,      // last known price derived from inventory (nullable)
        BigDecimal lastCostPrice,  // last known cost price derived from inventory (nullable)
        Integer totalStock,        // aggregated across inventory rows
        Integer minStock,
        Integer maxStock,
        BigDecimal maxDiscount,
        BigDecimal activeDiscount,     // Active discount % based on current date (0 if expired/not started)
        LocalDate discountStartDate,   // Discount start date (null = permanent)
        LocalDate discountEndDate,     // Discount end date (null = permanent)
        LocalDate expiryDate,
        String patientInstructions,
        String binLocation,
        String packSize
) { }
