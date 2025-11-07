package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rdp_suppliers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // JSON blob: {"contact":"...", "email":"...", "address":"..."}
    @Column(name = "contact_info")
    private String contactInfo;
}