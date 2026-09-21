package com.rdp.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.dto.SupplierReturnRequest;
import com.rdp.dto.SupplierReturnResponse;
import com.rdp.service.SupplierReturnService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/supplier-returns")
@RequiredArgsConstructor
public class SupplierReturnController {

    private final SupplierReturnService service;

    /**
     * Create a new supplier return with multiple items
     */
    @PostMapping
    public ResponseEntity<?> createReturn(@Valid @RequestBody SupplierReturnRequest request) {
        try {
            SupplierReturnResponse response = service.createReturn(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating supplier return: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating supplier return", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Error creating return: " + e.getMessage()));
        }
    }

    /**
     * Get all supplier returns (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<SupplierReturnResponse>> getAllReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupplierReturnResponse> response = service.getAllReturns(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get supplier returns for a specific supplier (paginated)
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<Page<SupplierReturnResponse>> getReturnsBySupplier(
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupplierReturnResponse> response = service.getReturnsBySupplier(supplierId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single supplier return by ID
     */
    @GetMapping("/{returnId}")
    public ResponseEntity<SupplierReturnResponse> getReturnById(@PathVariable Long returnId) {
        try {
            SupplierReturnResponse response = service.getReturnById(returnId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update an existing supplier return
     */
    @PutMapping("/{returnId}")
    public ResponseEntity<SupplierReturnResponse> updateReturn(
            @PathVariable Long returnId,
            @Valid @RequestBody SupplierReturnRequest request) {
        try {
            SupplierReturnResponse response = service.updateReturn(returnId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a supplier return
     */
    @DeleteMapping("/{returnId}")
    public ResponseEntity<Void> deleteReturn(@PathVariable Long returnId) {
        try {
            service.deleteReturn(returnId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Simple error response DTO
     */
    static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}
