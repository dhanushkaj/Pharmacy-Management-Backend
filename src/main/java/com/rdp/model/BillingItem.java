package com.rdp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rdp_billing_items",
        indexes = {
                @Index(name = "rdp_idx_billing_item_billing", columnList = "billing_id"),
                @Index(name = "rdp_idx_billing_item_product", columnList = "product_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillingItem extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_item_id")
    private Long billingItemId;

    @NotNull(message = "Billing is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    @JsonBackReference
    private Billing billing;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", message = "Subtotal must be at least 0")
    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @PrePersist
    @PreUpdate
    private void calculateSubtotal() {
        if (quantity != null && unitPrice != null) {
            subtotal = unitPrice.multiply(new BigDecimal(quantity));
        }
    }
}
