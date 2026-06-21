package com.rdp.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @Size(max = 20) String title,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String phone,
        @Email @Size(max = 100) String email,
        String address,
        @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
        @DecimalMax(value = "100.0", message = "Discount percentage must not exceed 100")
        BigDecimal discountPercentage,
        java.sql.Date birthday
) {}