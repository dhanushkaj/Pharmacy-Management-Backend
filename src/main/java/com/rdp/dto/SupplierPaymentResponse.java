package com.rdp.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPaymentResponse {
    private Long id;
    private String paymentReference;
    private Long invoiceId;
    private String invoiceNumber;
    private Long supplierId;
    private String supplierName;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private LocalDate chequeDate;
    private String chequeNumber;
    private String paymentStatus;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
