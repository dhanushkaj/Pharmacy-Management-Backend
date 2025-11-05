package com.rdp.dto;

import java.math.BigDecimal;

public record CreateInventoryRequest(
        BigDecimal price,
        BigDecimal costPrice,
        Integer stock,
        String batchNo
) { }