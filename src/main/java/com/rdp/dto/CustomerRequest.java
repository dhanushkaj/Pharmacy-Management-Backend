package com.rdp.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CustomerRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String phone,
        @Email @Size(max = 100) String email,
        String address,
        @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
        @DecimalMax(value = "100.0", message = "Discount percentage must not exceed 100")
        BigDecimal discountPercentage,
        java.sql.Date birthday
) {}