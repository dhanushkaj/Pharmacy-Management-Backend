package com.rdp.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceRequest {
    private String invoiceNumber;           // Supplier's invoice number
    private Long grnId;                     // GRN this invoice relates to
    private Long supplierId;                // Supplier ID
    private LocalDate invoiceDate;          // When supplier issued the invoice
    private BigDecimal invoiceAmount;       // Total billing amount
    private LocalDate paymentDueDate;       // Supplier's payment terms
}
