package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

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

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "patient_instructions", columnDefinition = "TEXT")
    private String patientInstructions;

    @Column(name = "bin_location", length = 50)
    private String binLocation;
}
