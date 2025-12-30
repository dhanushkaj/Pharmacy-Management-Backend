package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "rdp_inventory_items",
        uniqueConstraints = {
            @UniqueConstraint(name = "uc_inventory_product_price", columnNames = {"product_id", "price"})
        },
        indexes = {
                @Index(name = "rdp_idx_inventory_product_price", columnList = "product_id, price"),
                @Index(name = "rdp_idx_inventory_product", columnList = "product_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inv_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
    private java.math.BigDecimal price;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private java.math.BigDecimal costPrice;

    @Min(0)
    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "batch_no", length = 100)
    private String batchNo;

}
