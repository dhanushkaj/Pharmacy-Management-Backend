package com.rdp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record AttachReturnsRequest(
        @NotNull BigDecimal returnRefundTotal,
        @NotNull BigDecimal netPayable,
        @Valid List<ReturnRecordItem> returnRecords
) {
    public record ReturnRecordItem(
            Long originalBillingItemId,
            String originalBillingNumber,
            @NotNull Long productId,
            String productCode,
            String productName,
            @NotNull Integer quantity,
            @NotNull BigDecimal unitPrice,
            @NotNull BigDecimal refundAmount,
            BigDecimal discountPercentage
    ) {}
}
