package com.rdp.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
        @NotBlank String name,
        String genericName,
        Long categoryId,
        Long supplierId,
        String productCode,
        String barcode,
        @NotNull @DecimalMin("0.0") BigDecimal costPrice,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @Min(0) Integer stock,
        Integer minStock,
        Integer maxStock,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal maxDiscount,
        LocalDate expiryDate,
        String patientInstructions,
        String binLocation
) {}
