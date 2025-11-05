// src/main/java/com/rdp/controller/PurchaseOrderController.java
package com.rdp.controller;

import com.rdp.dto.PurchaseOrderRequest;
import com.rdp.dto.PurchaseOrderResponse;
import com.rdp.dto.UpdatePoItemQuantityRequest;
import com.rdp.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public List<PurchaseOrderResponse> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public PurchaseOrderResponse one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest req) {
        var created = service.create(req);
        return ResponseEntity.created(URI.create("/api/purchase-orders/" + created.id())).body(created);
    }

    @PatchMapping("/{orderId}/items/{itemId}")
    public PurchaseOrderResponse updateItemQty(@PathVariable Long orderId,
                                               @PathVariable Long itemId,
                                               @RequestBody Map<String, Integer> body) {
        Integer qty = body.get("quantity");
        if (qty == null) throw new IllegalArgumentException("quantity is required");
        return service.updateItemQuantity(orderId, itemId, qty);
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public PurchaseOrderResponse deleteItem(@PathVariable Long orderId,
                                            @PathVariable Long itemId) {
        return service.deleteItem(orderId, itemId);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok("Purchase Order Deleted " + id);
    }
}
