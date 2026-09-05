package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO for customer queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSpendDTO {
    private String customerName;
    private Double totalSpend;
    private LocalDate lastVisitDate;
    private Integer visitCount;
    private Double averageTransactionValue;
    private Integer daysSinceLastVisit;
    private String loyaltyStatus; // HIGH_VALUE, REGULAR, AT_RISK, INACTIVE
}
