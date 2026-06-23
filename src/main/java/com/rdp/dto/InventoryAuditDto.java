package com.rdp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryAuditDto(
    Long auditId,
    Long categoryId,
    String categoryName,
    Long exportedBy,
    String exportedByName,
    LocalDateTime exportedAt,
    Long uploadedBy,
    String uploadedByName,
    LocalDateTime uploadedAt,
    String status,
    String notes,
    Integer totalItems,
    Integer itemsAdjusted,
    List<InventoryAuditDetailDto> details
) {}
