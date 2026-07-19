package com.rdp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCountLineRequest {
    @NotNull(message = "Line ID is required")
    private Long id;
    private Integer physicalQty;
    private Boolean counted;
    private String lineComment;
}
