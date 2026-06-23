package com.rdp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rdp_inventory_audit_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAuditDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditDetailId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private InventoryAudit audit;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "system_qty_at_export", nullable = false)
    private Integer systemQtyAtExport;
    
    @Column(name = "physical_qty")
    private Integer physicalQty;
    
    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;
    
    @Column(name = "sell_price", precision = 15, scale = 2)
    private BigDecimal sellPrice;
    
    @Column(name = "variance")
    private Integer variance; // physicalQty - systemQtyAtExport
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Calculate variance if physical quantity is set
        if (physicalQty != null) {
            variance = physicalQty - systemQtyAtExport;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Recalculate variance if physical quantity changes
        if (physicalQty != null) {
            variance = physicalQty - systemQtyAtExport;
        }
    }
}
