package com.rdp.dto;

import java.util.List;

public record ProductMovementReportDto(
    List<String> months,
    List<ProductMovementDto> products
) {}
