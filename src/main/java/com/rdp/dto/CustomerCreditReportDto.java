package com.rdp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerCreditReportDto {
    private String customerName;
    private String phone;
    private String billingNumber;
    private LocalDateTime billingDate;
    private Double grandTotal;
    private boolean paid;
}