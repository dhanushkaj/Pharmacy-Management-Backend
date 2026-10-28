package com.rdp.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierPaymentRequest {
    private Long invoiceId;                 // Which invoice is being paid
    private Long supplierId;                // Supplier
    private BigDecimal paymentAmount;       // How much we're paying
    private LocalDate paymentDate;          // When payment was made
    private String paymentMethod;           // CASH, CHECK, BANK_TRANSFER
    private LocalDate chequeDate;           // Date on cheque (required if method=CHECK)
    private String chequeNumber;            // Cheque number (optional)
    private String remarks;                 // Additional notes
}
