package com.rdp.dto;

import com.rdp.model.BinType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMovementRequest {
    @NotNull
    private java.math.BigDecimal price;
    @NotNull
    private BinType fromBin;
    @NotNull
    private BinType toBin;
    @NotNull
    @Min(1)
    private Integer quantity;
    private String referenceType;
    private String referenceId;
    private String performedBy;
    private String remarks;
    private String batchNo;
}
