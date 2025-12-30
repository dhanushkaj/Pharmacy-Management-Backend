package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "rdp_store_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreSettings extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settings_id")
    private Long settingsId;

    @NotBlank(message = "Store name is required")
    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;

    @NotBlank(message = "Address is required")
    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @NotBlank(message = "Phone is required")
    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "logo", columnDefinition = "TEXT")
    private String logo; // Base64 encoded image

    // Only one settings record should exist (singleton pattern)
    // We'll enforce this in the service layer
}
