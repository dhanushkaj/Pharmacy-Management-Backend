package com.rdp.dto;

import java.math.BigDecimal;

public record CustomerResponse(
        Long customerId,
        String name,
        String phone,
        String email,
        String address,
        BigDecimal discountPercentage,
        java.sql.Date birthday
) {}