package com.rdp.model;

import com.rdp.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "rdp_customers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email"}),
                @UniqueConstraint(columnNames = {"phone"})
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @Email
    @Size(max = 100)
    @Column(length = 100)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Discount percentage must not exceed 100")
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal discountPercentage = java.math.BigDecimal.ZERO;
}
