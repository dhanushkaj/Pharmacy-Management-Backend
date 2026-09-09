package com.rdp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rdp_supplier_payments",
        indexes = { @Index(name = "rdp_payment_ref_idx", columnList = "payment_reference", unique = true) })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPayment extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 50)
    private String paymentReference;        // Auto-generated: PAY-SUPPLIER-20260907-0001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;                // Links to the supplier's invoice

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;              // Reference to supplier

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;       // How much we're paying

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;          // When the payment was made

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;           // CASH, CHECK, BANK_TRANSFER, etc.

    // Cheque-specific fields
    @Column(name = "cheque_date")
    private LocalDate chequeDate;           // Date on the cheque (MANDATORY if method is CHECK)

    @Column(name = "cheque_number", length = 50)
    private String chequeNumber;            // Cheque number (OPTIONAL)

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;           // PENDING, COMPLETED, CLEARED, etc.

    @Column(name = "remarks", length = 500)
    private String remarks;                 // Additional notes

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;               // User who recorded payment

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paymentStatus == null) {
            paymentStatus = "PENDING";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
