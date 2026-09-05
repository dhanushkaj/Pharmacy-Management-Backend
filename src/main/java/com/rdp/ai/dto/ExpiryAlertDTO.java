package com.rdp.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * DTO for expiry management queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryAlertDTO {
    private String productName;
    private String category;
    private Integer quantityRemaining;
    private LocalDate expiryDate;
    private Integer daysUntilExpiry;
    private Double estimatedValue;
    private String status; // EXPIRED, EXPIRING_SOON, SAFE
}
