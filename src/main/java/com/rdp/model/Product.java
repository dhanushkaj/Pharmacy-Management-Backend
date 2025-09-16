package com.rdp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;                  // Product name

    @Column(name = "generic_name", length = 100)
    private String genericName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "product_code", unique = true, length = 50)
    private String productCode;

    @Column(name = "barcode", unique = true, length = 50)
    private String barcode;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "cost_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal costPrice;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Min(0)
    @Column(name = "stock")
    private Integer stock;

    @Min(0)
    @Column(name = "min_stock")
    private Integer minStock;

    @Column(name = "max_stock")
    private Integer maxStock;

    @DecimalMin("0.0") @DecimalMax("100.0")
    @Column(name = "max_discount", precision = 5, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "patient_instructions", columnDefinition = "TEXT")
    private String patientInstructions;

    @Column(name = "bin_location", length = 50)
    private String binLocation;
}
