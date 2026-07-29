package com.rdp.dto;

import com.rdp.model.CountSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountSessionDTO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private Integer versionNumber;
    private CountSessionStatus status;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private Long submittedById;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private Long rejectedById;
    private String rejectedByName;
    private LocalDateTime rejectedAt;
    private String rejectedReason;
    private String overallComment;
    private LocalDateTime updatedAt;
    private List<InventoryCountLineDTO> lines;
}
