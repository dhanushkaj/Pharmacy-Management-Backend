package com.rdp.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private Long grnId;
    private Long supplierId;
    private String supplierName;
    private LocalDate invoiceDate;
    private BigDecimal invoiceAmount;
    private LocalDate paymentDueDate;
    private String paymentStatus;           // UNPAID, PARTIAL, PAID
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
