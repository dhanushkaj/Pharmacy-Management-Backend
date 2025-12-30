package com.rdp.dto;

import java.util.List;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkImportResponse {
    private int ok;
    private int failed;
    private List<String> errors; // e.g., "Row 3: productCode already exists: AB1234"
}