package com.rdp.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BestCustomerDTO {
    private Long customerId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String title;
    private BigDecimal totalRevenue;
    private Integer purchaseFrequency;
    private LocalDateTime lastPurchaseDate;
    private BigDecimal averageOrderValue;
    private Integer rank;
}
