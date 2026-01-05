// Removed duplicate misplaced getters. All are defined inside the Grn class below.
package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import com.rdp.model.enums.GrnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rdp_grns",
        indexes = { @Index(name = "rdp_grn_code_idx", columnList = "grn_code", unique = true) })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Grn extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

            public Boolean getPaid() { return this.paid; }
            public LocalDate getPaymentDueDate() { return this.paymentDueDate; }
            public Integer getPaymentDueDays() { return this.paymentDueDays; }
            public LocalDate getChequeDate() { return this.chequeDate; }
    @Column(name = "status", nullable = false)
    private GrnStatus status;

    @Column(name = "approved_user")
    private String approvedUser;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "rejected_reason")
    private String rejectedReason;

    @Builder.Default
    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrnItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Payment fields for payment alerts
    @Column(name = "paid")
    private Boolean paid = false;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    @Column(name = "payment_due_days")
    private Integer paymentDueDays;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;
    
    @Column(name="grn_code")
    private String grnCode;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(GrnItem item) {
        this.items.add(item);
        item.setGrn(this);
    }
}