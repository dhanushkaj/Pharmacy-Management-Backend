package com.rdp.controller;

import com.rdp.dto.CustomerRequest;
import com.rdp.dto.CustomerResponse;
import com.rdp.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service = service; }

    @GetMapping
    public List<CustomerResponse> all() {
        return service.findAll();
    }

    @GetMapping("/page")
    public Page<CustomerResponse> allPaginated(Pageable pageable) {
        return service.findAllPaginated(pageable);
    }

    @GetMapping("/{id}")
    public CustomerResponse one(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest req) {
        var created = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/customers/" + created.customerId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable("id") Long id, @Valid @RequestBody CustomerRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.delete(id));
    }
}