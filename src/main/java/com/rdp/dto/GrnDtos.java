package com.rdp.dto;

import com.rdp.model.enums.GrnStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public class GrnDtos {

    public record GrnItemRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer receivedQuantity,
            BigDecimal unitCost,
            BigDecimal price
    ) {}

    public record CreateGrnRequest(
            @NotNull Long purchaseOrderId,
            @NotEmpty List<GrnItemRequest> items,
            Boolean paid,
            LocalDate paymentDueDate,
            Integer paymentDueDays,
            LocalDate chequeDate
    ) {}

    public record RejectGrnRequest(
            @NotBlank String reason
    ) {}

    public record ApproveGrnRequest(
            @NotBlank String approvedUser
    ) {}

    public record GrnItemResponse(
            Long id,
            Long productId,
            String productName,
            Integer receivedQuantity,
            BigDecimal unitCost,
            BigDecimal price
    ) {}

    public record GrnResponse(
            Long id,
            String grnCode,
            Long purchaseOrderId,
            String purchaseOrderCode,
            String supplierName,
            LocalDateTime createdAt,
            LocalDateTime approvedDate,
            String approvedUser,
            GrnStatus status,
            String rejectedReason,
            Boolean paid,
            LocalDate paymentDueDate,
            Integer paymentDueDays,
            LocalDate chequeDate,
            List<GrnItemResponse> items
    ) {}
}