package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rdp_inventory_returns",
        indexes = {
                @Index(name = "rdp_idx_return_product", columnList = "product_id"),
                @Index(name = "rdp_idx_return_type", columnList = "return_type"),
                @Index(name = "rdp_idx_return_date", columnList = "return_date")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryReturn extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Return type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false, length = 20)
    private ReturnType returnType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotBlank(message = "Reason is required")
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @Column(name = "return_date", nullable = false)
    @Builder.Default
    private LocalDateTime returnDate = LocalDateTime.now();

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "notes", length = 1000)
    private String notes;

    @PrePersist
    @PreUpdate
    private void calculateTotalAmount() {
        if (quantity != null && unitPrice != null) {
            totalAmount = unitPrice.multiply(new BigDecimal(quantity));
        }
    }

    public enum ReturnType {
        FROM_CUSTOMER,  // Customer returns product → Add to inventory
        TO_SUPPLIER     // Return product to supplier → Reduce from inventory
    }
}
