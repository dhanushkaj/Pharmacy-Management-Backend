package com.rdp.controller;

import com.rdp.dto.SupplierPaymentRequest;
import com.rdp.dto.SupplierPaymentResponse;
import com.rdp.service.SupplierPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplier-payments")
public class SupplierPaymentController {

    private final SupplierPaymentService paymentService;

    public SupplierPaymentController(SupplierPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Record a payment for supplier invoice
     */
    @PostMapping
    public ResponseEntity<SupplierPaymentResponse> recordPayment(
            @RequestBody SupplierPaymentRequest request,
            Authentication authentication) {
        String currentUser = authentication != null ? authentication.getName() : "System";
        SupplierPaymentResponse response = paymentService.recordPayment(request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all supplier payments
     */
    @GetMapping
    public ResponseEntity<List<SupplierPaymentResponse>> getAllPayments() {
        List<SupplierPaymentResponse> responses = paymentService.getAllPayments();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<SupplierPaymentResponse> getPaymentById(@PathVariable Long paymentId) {
        SupplierPaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by reference
     */
    @GetMapping("/reference/{paymentReference}")
    public ResponseEntity<SupplierPaymentResponse> getPaymentByReference(@PathVariable String paymentReference) {
        SupplierPaymentResponse response = paymentService.getPaymentByReference(paymentReference);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for an invoice
     */
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<SupplierPaymentResponse>> getPaymentsByInvoice(@PathVariable Long invoiceId) {
        List<SupplierPaymentResponse> responses = paymentService.getPaymentsByInvoice(invoiceId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all payments for a supplier
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<SupplierPaymentResponse>> getPaymentsBySupplier(@PathVariable Long supplierId) {
        List<SupplierPaymentResponse> responses = paymentService.getPaymentsBySupplier(supplierId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get payments by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SupplierPaymentResponse>> getPaymentsByStatus(@PathVariable String status) {
        List<SupplierPaymentResponse> responses = paymentService.getPaymentsByStatus(status);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get payments by date range (for Day-End Report reconciliation)
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<SupplierPaymentResponse>> getPaymentsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<SupplierPaymentResponse> responses = paymentService.getPaymentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get total payments by date range and method (useful for cash/bank reconciliation)
     */
    @GetMapping("/totals/date-range")
    public ResponseEntity<Map<String, BigDecimal>> getTotalPayments(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String paymentMethod) {
        BigDecimal total = paymentService.getTotalPaymentsByDateRange(startDate, endDate, paymentMethod);
        return ResponseEntity.ok(Map.of("total", total));
    }
}
