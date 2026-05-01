package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "stock_movement",
       indexes = {@Index(name = "idx_sm_product_batch_date", columnList = "product_id, batch_no, created_at")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockMovement extends BaseAuditableEntity {
    @NotNull
    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private java.math.BigDecimal price;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotNull
    @Column(name = "batch_no", nullable = false)
    private String batchNo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "from_bin", nullable = false)
    private BinType fromBin;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_bin", nullable = false)
    private BinType toBin;

    @NotNull
    //@Min(value = 1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
