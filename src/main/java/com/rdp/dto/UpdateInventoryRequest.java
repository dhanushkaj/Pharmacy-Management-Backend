package com.rdp.dto;

import java.math.BigDecimal;
public record UpdateInventoryRequest(
        BigDecimal price,
        BigDecimal costPrice,
        Integer stock,
        String batchNo
) {}