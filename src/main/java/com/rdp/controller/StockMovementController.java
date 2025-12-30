package com.rdp.controller;

import com.rdp.dto.CreateMovementRequest;
import com.rdp.dto.StockMovementDto;
import com.rdp.service.StockMovementService;
import com.rdp.service.InventorySummaryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products/{productId}")
public class StockMovementController {

    private final StockMovementService service;
    private final InventorySummaryService summaryService;

    public StockMovementController(StockMovementService service, InventorySummaryService summaryService) {
        this.service = service;
        this.summaryService = summaryService;
    }

    @GetMapping("/bin-movements")
    public ResponseEntity<Page<StockMovementDto>> getMovements(
            @PathVariable Long productId,
            @RequestParam Optional<String> batchNo,
            @RequestParam Optional<Long> startDateMillis,
            @RequestParam Optional<Long> endDateMillis,
            @RequestParam Optional<List<String>> binTypes,
            @RequestParam Optional<String> referenceType,
            @RequestParam Optional<String> performedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Optional<Date> start = startDateMillis.map(Date::new);
        Optional<Date> end = endDateMillis.map(Date::new);
        Page<StockMovementDto> result = service.getMovements(productId, batchNo, start, end, binTypes, referenceType, performedBy, page, size);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bin-movements")
    public ResponseEntity<StockMovementDto> createMovement(@PathVariable Long productId, @Valid @RequestBody CreateMovementRequest req) {
        StockMovementDto dto = service.createMovement(productId, req);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/inventory-summary")
    public ResponseEntity<?> getInventorySummary(@PathVariable Long productId) {
        return ResponseEntity.ok(summaryService.getInventorySummary(productId));
    }
}
