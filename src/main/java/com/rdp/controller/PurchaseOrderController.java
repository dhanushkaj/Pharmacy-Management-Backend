// src/main/java/com/rdp/controller/PurchaseOrderController.java
package com.rdp.controller;

import com.rdp.dto.PurchaseOrderRequest;
import com.rdp.dto.PurchaseOrderResponse;
import com.rdp.dto.UpdatePoItemQuantityRequest;
import com.rdp.dto.UpdatePurchaseOrderRequest;
import com.rdp.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @GetMapping
    public ResponseEntity<Page<PurchaseOrderResponse>> all(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse one(@PathVariable("id") Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest req) {
        var created = service.create(req);
        return ResponseEntity.created(URI.create("/api/purchase-orders/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> update(
            @PathVariable("id") Long id,
            @RequestBody UpdatePurchaseOrderRequest req) {
        var updated = service.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{orderId}/items/{itemId}")
    public PurchaseOrderResponse updateItemQty(@PathVariable("orderId") Long orderId,
                                               @PathVariable("itemId") Long itemId,
                                               @RequestBody Map<String, Integer> body) {
        Integer qty = body.get("quantity");
        if (qty == null) throw new IllegalArgumentException("quantity is required");
        return service.updateItemQuantity(orderId, itemId, qty);
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public PurchaseOrderResponse deleteItem(@PathVariable("orderId") Long orderId,
                                            @PathVariable("itemId") Long itemId) {
        return service.deleteItem(orderId, itemId);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.ok("Purchase Order Deleted " + id);
    }
}
