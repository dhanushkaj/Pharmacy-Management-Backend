package com.rdp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rdp_grn_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GrnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private Grn grn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @Min(1)
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity;

    // Snapshot of cost at time of receiving
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;
}