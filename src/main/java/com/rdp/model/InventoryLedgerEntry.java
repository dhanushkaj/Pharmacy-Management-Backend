package com.rdp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rdp_inventory_ledger_entries", indexes = {
    @Index(name = "rdp_idx_ledger_product", columnList = "product_id"),
    @Index(name = "rdp_idx_ledger_reason", columnList = "reason_type"),
    @Index(name = "rdp_idx_ledger_reference", columnList = "reference_session_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    // Signed quantity change: positive = increase, negative = decrease
    @Column(nullable = false)
    private Integer changeQty;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementReasonType reasonType;
    
    // Reference to the session that triggered this ledger entry
    @Column(name = "reference_session_id", nullable = true)
    private Long referenceSessionId;
    
    // System qty before and after for audit trail
    @Column(nullable = false)
    private Integer systemQtyBefore;
    
    @Column(nullable = false)
    private Integer systemQtyAfter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
