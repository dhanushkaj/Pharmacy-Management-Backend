// src/main/java/com/rdp/controller/ProductController.java
package com.rdp.controller;

import com.rdp.dto.BulkImportResponse;
import com.rdp.dto.ProductCsvRequest;
import com.rdp.dto.ProductRequest;
import com.rdp.dto.ProductResponse;
import com.rdp.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ProductResponse one(@PathVariable("id") Long id) { return service.findById(id); }

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

    @PostMapping("/bulk-csv")
    public ResponseEntity<BulkImportResponse> bulkCsv(@RequestBody List<@Valid ProductCsvRequest> items) {
        var result = service.bulkCreateFromCsv(items);
        return ResponseEntity.ok(result); // 200 with {ok, failed, errors}
    }

    @GetMapping("/search")
    public List<ProductResponse> search(@RequestParam("q") String q,
                                        @RequestParam(value = "categoryId", required = false) Long categoryId) {
        return service.search(q, categoryId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductResponse update(@PathVariable("id") Long id, @Valid @RequestBody ProductRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}
