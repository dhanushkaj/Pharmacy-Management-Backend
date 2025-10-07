package com.rdp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rdp_inventory_items",
        indexes = {
                @Index(name = "rdp_idx_inventory_product_price", columnList = "product_id, price"),
                @Index(name = "rdp_idx_inventory_product", columnList = "product_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inv_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private java.math.BigDecimal price;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private java.math.BigDecimal costPrice;

    @Min(0)
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}
