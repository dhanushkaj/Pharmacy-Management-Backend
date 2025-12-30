package com.rdp.service;

import com.rdp.dto.InventorySummaryDto;
import com.rdp.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventorySummaryService {

    private final StockMovementRepository repository;

    public InventorySummaryService(StockMovementRepository repository) {
        this.repository = repository;
    }

    public List<InventorySummaryDto> getInventorySummary(Long productId) {
        // For each batch, compute inventory quantity by aggregation
        // We'll query all movements for product and aggregate by batch
        List<com.rdp.model.StockMovement> all = repository.findAll((root, query, cb) -> cb.equal(root.get("productId"), productId));

        return all.stream()
                .collect(Collectors.groupingBy(sm -> sm.getBatchNo() == null ? "" : sm.getBatchNo()))
                .entrySet().stream().map(e -> {
                    String batch = e.getKey();
                    int qty = 0;
                    for (com.rdp.model.StockMovement sm : e.getValue()) {
                        if (sm.getToBin() == com.rdp.model.BinType.INVENTORY) qty += sm.getQuantity();
                        if (sm.getFromBin() == com.rdp.model.BinType.INVENTORY) qty -= sm.getQuantity();
                    }
                    // expiry date: try to lookup from product_batch table if exists -- not implemented here
                    InventorySummaryDto dto = InventorySummaryDto.builder()
                            .productId(productId)
                            .batchNo(batch)
                            .expiryDate(null)
                            .inventoryQuantity(qty)
                            .nearExpiry(false)
                            .build();
                    return dto;
                }).collect(Collectors.toList());
    }
}
