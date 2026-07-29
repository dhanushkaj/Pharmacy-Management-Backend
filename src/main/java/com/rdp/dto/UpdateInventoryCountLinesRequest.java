package com.rdp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInventoryCountLinesRequest {
    @NotNull(message = "Lines are required")
    private List<UpdateCountLineRequest> lines;
}
