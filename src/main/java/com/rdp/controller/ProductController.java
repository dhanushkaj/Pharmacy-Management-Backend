// src/main/java/com/rdp/controller/ProductController.java
package com.rdp.controller;

import com.rdp.dto.BulkImportResponse;
import com.rdp.dto.ProductRequest;
import com.rdp.dto.ProductResponse;
import com.rdp.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public List<ProductResponse> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public ProductResponse one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        var created = service.create(req);
        return ResponseEntity.created(URI.create("/api/products/" + created.productId())).body(created);
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse> bulk(@RequestBody List<@Valid ProductRequest> items) {
        var result = service.bulkCreate(items);
        return ResponseEntity.ok(result); // 200 with {ok, failed, errors}
    }


    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}
