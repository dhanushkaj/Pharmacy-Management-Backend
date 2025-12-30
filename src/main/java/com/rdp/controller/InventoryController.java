package com.rdp.controller;

import com.rdp.dto.CreateInventoryRequest;
import com.rdp.dto.UpdateInventoryRequest;
import com.rdp.model.InventoryItem;
import com.rdp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products/{productId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryDto>> list(@PathVariable("productId") Long productId) {
        var items = inventoryService.listByProduct(productId);
        var dtos = items.stream().map(InventoryDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<InventoryDto> create(@PathVariable("productId") Long productId, @RequestBody CreateInventoryRequest req) {
        var it = inventoryService.addOrIncrement(productId, req);
        return ResponseEntity.ok(InventoryDto.from(it));
    }

    @PutMapping("/{invId}")
    public ResponseEntity<InventoryDto> update(@PathVariable("productId") Long productId, @PathVariable("invId") Long invId, @RequestBody UpdateInventoryRequest req) {
        var it = inventoryService.updateInventory(productId, invId, req);
        return ResponseEntity.ok(InventoryDto.from(it));
    }

    @DeleteMapping("/{invId}")
    public ResponseEntity<String> delete(@PathVariable("productId") Long productId, @PathVariable("invId") Long invId) {
        inventoryService.deleteInventory(productId, invId);
        return ResponseEntity.ok("Deleted");
    }

    // Simple DTO for API responses (avoid returning entity directly)
    public static record InventoryDto(Long id, String batchNo, String price, String costPrice, Integer stock, String createdAt, String updatedAt) {
        static InventoryDto from(InventoryItem it) {
            return new InventoryDto(
                    it.getId(),
                    it.getBatchNo(),
                    it.getPrice() == null ? null : it.getPrice().toPlainString(),
                    it.getCostPrice() == null ? null : it.getCostPrice().toPlainString(),
                    it.getStock(),
                    it.getCreatedAt() == null ? null : it.getCreatedAt().toString(),
                    it.getUpdatedAt() == null ? null : it.getUpdatedAt().toString()
            );
        }
    }
}
