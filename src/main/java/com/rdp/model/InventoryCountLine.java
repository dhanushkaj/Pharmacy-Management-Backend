package com.rdp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rdp_inventory_count_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountLine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InventoryCountSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;
    
    @Column(nullable = false)
    private String productCode;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private BigDecimal sellPrice;
    
    // Snapshot at count time
    @Column(nullable = false)
    private Integer systemQtyAtCount;
    
    // Clerk enters this during count
    @Column(nullable = true)
    private Integer physicalQty;
    
    // Automatically calculated: physicalQty - systemQtyAtCount
    @Column(nullable = true)
    private Integer variance;
    
    // Marks if this line has been counted (not yet counted vs counted as zero)
    @Column(nullable = false)
    @Builder.Default
    private Boolean counted = false;
    
    @Column(columnDefinition = "TEXT", nullable = true)
    private String lineComment;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
