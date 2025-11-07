package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import com.rdp.model.enums.GrnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(name = "grn_code", nullable = false, unique = true, length = 30)
    private String grnCode; // e.g., GRN-20251021-0001

    @Enumerated(EnumType.STRING)
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