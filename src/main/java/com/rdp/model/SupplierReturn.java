package com.rdp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.rdp.audit.BaseAuditableEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rdp_supplier_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierReturn extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "return_number", unique = true, nullable = false, length = 50)
    private String returnNumber;

    @Column(name = "return_date", nullable = false)
    private LocalDateTime returnDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "total_return_amount", precision = 15, scale = 2)
    private BigDecimal totalReturnAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "supplierReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SupplierReturnItem> returnItems = new ArrayList<>();

    /**
     * Calculate total return amount from items
     */
    public void calculateTotalReturnAmount() {
        this.totalReturnAmount = returnItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Add an item to this return
     */
    public void addReturnItem(SupplierReturnItem item) {
        item.setSupplierReturn(this);
        this.returnItems.add(item);
        calculateTotalReturnAmount();
    }

    /**
     * Remove an item from this return
     */
    public void removeReturnItem(SupplierReturnItem item) {
        this.returnItems.remove(item);
        item.setSupplierReturn(null);
        calculateTotalReturnAmount();
    }
}
