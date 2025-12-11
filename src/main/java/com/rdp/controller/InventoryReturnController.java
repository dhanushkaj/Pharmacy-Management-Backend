package com.rdp.controller;

import com.rdp.dto.InventoryReturnRequest;
import com.rdp.dto.InventoryReturnResponse;
import com.rdp.model.InventoryReturn;
import com.rdp.service.InventoryReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/inventory-returns")
@RequiredArgsConstructor
public class InventoryReturnController {

    private final InventoryReturnService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PHARMACIST')")
    public ResponseEntity<InventoryReturnResponse> createReturn(@Valid @RequestBody InventoryReturnRequest request) {
        InventoryReturnResponse response = service.createReturn(request);
        return ResponseEntity.created(URI.create("/api/inventory-returns/" + response.returnId()))
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PHARMACIST')")
    public Page<InventoryReturnResponse> getAllReturns(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return service.getAllReturns(pageable);
    }

    @GetMapping("/type/{returnType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PHARMACIST')")
    public Page<InventoryReturnResponse> getReturnsByType(
            @PathVariable("returnType") InventoryReturn.ReturnType returnType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return service.getReturnsByType(returnType, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PHARMACIST')")
    public InventoryReturnResponse getReturnById(@PathVariable("id") Long id) {
        return service.getReturnById(id);
    }
}
