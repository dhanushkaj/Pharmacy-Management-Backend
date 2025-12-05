package com.rdp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for CSV bulk upload of products.
 * Uses names instead of IDs for category and supplier.
 * Product code and barcode are auto-generated if not provided.
 */
public record ProductCsvRequest(
        @NotBlank(message = "Product name is required")
        String name,
        
        @NotBlank(message = "Generic name is required")
        String genericName,
        
        @NotBlank(message = "Category name is required")
        String categoryName,
        
        @NotBlank(message = "Supplier name is required")
        String supplierName,
        
        // Optional fields
        BigDecimal costPrice,
        BigDecimal price,
        
        @Min(value = 0, message = "Stock must be >= 0")
        Integer stock,
        
        @Min(value = 0, message = "Min stock must be >= 0")
        Integer minStock,
        
        @Min(value = 0, message = "Max stock must be >= 0")
        Integer maxStock,
        
        @DecimalMin(value = "0.0", message = "Max discount must be >= 0")
        BigDecimal maxDiscount,
        
        LocalDate expiryDate,
        String patientInstructions,
        String binLocation
) {
    /**
     * Create with default values for optional fields
     */
    public ProductCsvRequest {
        // Set defaults for null values
        stock = (stock == null) ? 0 : stock;
        minStock = (minStock == null) ? 0 : minStock;
        maxStock = (maxStock == null) ? 0 : maxStock;
        maxDiscount = (maxDiscount == null) ? BigDecimal.ZERO : maxDiscount;
    }
}
