package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rdp_purchase_orders",
        indexes = { @Index(name = "rdp_po_order_code", columnList = "order_code", unique = true) })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrder extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id")
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;                  // e.g. PO-SUPPLIERNAME-20251126-0001

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;           // order created date/time

    @Column(name = "needed_date")
    private LocalDate neededDate;              // when stock is needed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;              // optional snapshot

    public void addItem(PurchaseOrderItem item) {
        item.setPurchaseOrder(this);
        this.items.add(item);
    }
}
