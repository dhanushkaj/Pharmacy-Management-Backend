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
@Table(name = "rdp_billing_return_records",
        indexes = {
                @Index(name = "rdp_idx_brr_billing", columnList = "billing_id"),
                @Index(name = "rdp_idx_brr_original_billing", columnList = "original_billing_number")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillingReturnRecord extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_return_record_id")
    private Long billingReturnRecordId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    @JsonBackReference
    private Billing billing;

    @Column(name = "original_billing_item_id")
    private Long originalBillingItemId;

    @Column(name = "original_billing_number", length = 50)
    private String originalBillingNumber;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "product_name", length = 255)
    private String productName;

    @NotNull
    @Min(value = 1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;
}
