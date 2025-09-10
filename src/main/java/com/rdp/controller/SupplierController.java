package com.rdp.controller;

import com.rdp.dto.SupplierRequest;
import com.rdp.dto.SupplierResponse;
import com.rdp.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService service;
    public SupplierController(SupplierService service) { this.service = service; }

    @GetMapping
    public List<SupplierResponse> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public SupplierResponse one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest req) {
        var created = service.create(req);
        return ResponseEntity.created(URI.create("/api/suppliers/" + created.supplierId())).body(created);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return ResponseEntity.ok(service.delete(id)); // "Supplier Deleted <id>"
    }
}