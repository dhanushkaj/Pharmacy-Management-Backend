package com.rdp.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rdp_billings",
        indexes = {
                @Index(name = "rdp_idx_billing_customer", columnList = "customer_id"),
                @Index(name = "rdp_idx_billing_date", columnList = "billing_date"),
                @Index(name = "rdp_idx_billing_number", columnList = "billing_number")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Billing extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_id")
    private Long billingId;

    @Column(name = "billing_number", nullable = false, unique = true, length = 50)
    private String billingNumber;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Billing date is required")
    @Column(name = "billing_date", nullable = false)
    @Builder.Default
    private LocalDateTime billingDate = LocalDateTime.now();

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", message = "Subtotal must be at least 0")
    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Discount amount must be at least 0")
    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull(message = "Grand total is required")
    @DecimalMin(value = "0.0", message = "Grand total must be at least 0")
    @Column(name = "grand_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "payment_method", length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @DecimalMin(value = "0.0", message = "Amount received must be at least 0")
    @Column(name = "amount_received", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountReceived = BigDecimal.ZERO;

    @Column(name = "balance_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Column(name = "is_printed")
    @Builder.Default
    private Boolean isPrinted = false;

    @Column(name = "paid", nullable = false)
    @Builder.Default
    private boolean paid = false;

    public boolean isPaid() {
        return paid;
    }
    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<BillingItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BillingReturnRecord> returnRecords = new ArrayList<>();

    @Column(name = "return_refund_total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal returnRefundTotal = BigDecimal.ZERO;

    @Column(name = "net_payable", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal netPayable = BigDecimal.ZERO;

    public void addItem(BillingItem item) {
        items.add(item);
        item.setBilling(this);
    }

    public void removeItem(BillingItem item) {
        items.remove(item);
        item.setBilling(null);
    }

    // Removed @PrePersist/@PreUpdate calculation to allow manual discountAmount from frontend

    public enum PaymentMethod {
        CASH,
        CARD,
        MOBILE_PAYMENT,
        ONLINE_TRANSFER,
        CREDIT,
        CHEQUE,
        OTHER,
        OLD_MANUAL
    }
}
