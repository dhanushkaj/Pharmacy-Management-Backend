package com.rdp.model;

import java.time.LocalDate;

import com.rdp.audit.BaseAuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rdp_products",
        indexes = { @Index(name = "rdp_idx_product_code", columnList = "product_code"),
                @Index(name = "rdp_idx_barcode", columnList = "barcode") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "generic_name", length = 100)
    private String genericName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @NotBlank
    @Column(name = "product_code", nullable = false, length = 50, unique = true)
    private String productCode;

    @NotBlank
    @Column(name = "barcode", nullable = false, length = 50, unique = true)
    private String barcode;

    @Min(0)
    @Column(name = "min_stock")
    private Integer minStock;

    @Column(name = "max_stock")
    private Integer maxStock;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0", message = "Max Discount must be between 0.0 and 100.0")
    @Column(name = "max_discount", precision = 5, scale = 2)
    private java.math.BigDecimal maxDiscount;

    @Column(name = "discount_start_date")
    private LocalDate discountStartDate;

    @Column(name = "discount_end_date")
    private LocalDate discountEndDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "patient_instructions", columnDefinition = "TEXT")
    private String patientInstructions;

    @Column(name = "bin_location", length = 50)
    private String binLocation;

    @Column(name = "pack_size", length = 50)
    private String packSize;

    /**
     * Get the active discount percentage for this product based on today's date.
     * 
     * Logic:
     * - If no discount set, return 0
     * - If discount has no date range (both null), return discount (permanent)
     * - If discount has date range, check if today falls within:
     *   - If today < startDate: return 0 (hasn't started)
     *   - If today > endDate: return 0 (expired)
     *   - If today is within range: return discount
     *
     * @return Active discount percentage (0-100), or BigDecimal.ZERO if no active discount
     */
    public java.math.BigDecimal getActiveDiscount() {
        // No discount set
        if (maxDiscount == null || maxDiscount.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return java.math.BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();

        // Permanent discount (no date range)
        if (discountStartDate == null && discountEndDate == null) {
            return maxDiscount;
        }

        // Seasonal discount - check date range
        if (discountStartDate != null && today.isBefore(discountStartDate)) {
            return java.math.BigDecimal.ZERO; // Discount hasn't started yet
        }

        if (discountEndDate != null && today.isAfter(discountEndDate)) {
            return java.math.BigDecimal.ZERO; // Discount has expired
        }

        // Today is within the discount period (or only startDate/endDate is set)
        return maxDiscount;
    }
}
