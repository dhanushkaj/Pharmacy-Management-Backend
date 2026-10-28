package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rdp_invoices",
        indexes = { @Index(name = "rdp_invoice_number_idx", columnList = "invoice_number", unique = true) })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;           // From supplier's invoice

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private Grn grn;                        // Links to the GRN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;              // Reference to supplier

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;          // When supplier issued the invoice

    @Column(name = "invoice_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal invoiceAmount;       // Total billing amount from supplier

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;       // Supplier's payment terms

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;           // UNPAID, PARTIAL, PAID

    @Column(name = "amount_paid", precision = 12, scale = 2)
    private BigDecimal amountPaid;          // Total amount paid so far (default 0.00)

    @Column(name = "amount_remaining", precision = 12, scale = 2)
    private BigDecimal amountRemaining;     // Invoice amount - paid

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paymentStatus == null) {
            paymentStatus = "UNPAID";
        }
        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO;
        }
        if (amountRemaining == null) {
            amountRemaining = invoiceAmount;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Recalculate remaining amount
        if (invoiceAmount != null && amountPaid != null) {
            amountRemaining = invoiceAmount.subtract(amountPaid);
        }
    }
}
