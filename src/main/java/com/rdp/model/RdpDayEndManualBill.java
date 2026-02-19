package com.rdp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rdp_day_end_manual_bill")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RdpDayEndManualBill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", nullable = false, length = 50)
    private String billNumber;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "report_date", nullable = false)
    private String reportDate; // yyyy-MM-dd

    @Column(name = "created_by", nullable = false)
    private String createdBy;
}
