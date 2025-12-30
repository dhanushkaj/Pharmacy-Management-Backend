package com.rdp.dto;

import java.time.LocalDate;

/**
 * DTO for updating purchase order fields (supplier and needed date).
 */
public record UpdatePurchaseOrderRequest(
        Long supplierId,
        LocalDate neededDate
) {}
