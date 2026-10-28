package com.rdp.controller;

import com.rdp.dto.InvoiceRequest;
import com.rdp.dto.InvoiceResponse;
import com.rdp.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Create a new supplier invoice
     */
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@RequestBody InvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get invoice by ID
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long invoiceId) {
        InvoiceResponse response = invoiceService.getInvoiceById(invoiceId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get invoice by invoice number
     */
    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        InvoiceResponse response = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get invoice for a GRN
     */
    @GetMapping("/grn/{grnId}")
    public ResponseEntity<InvoiceResponse> getInvoiceByGrn(@PathVariable Long grnId) {
        InvoiceResponse response = invoiceService.getInvoiceByGrn(grnId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all invoices for a supplier
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesBySupplier(@PathVariable Long supplierId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesBySupplier(supplierId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get unpaid invoices for a supplier (for payment screen)
     */
    @GetMapping("/supplier/{supplierId}/unpaid")
    public ResponseEntity<List<InvoiceResponse>> getUnpaidInvoicesBySupplier(@PathVariable Long supplierId) {
        List<InvoiceResponse> responses = invoiceService.getUnpaidInvoicesBySupplier(supplierId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get ALL unpaid invoices across all suppliers (for payment list view)
     */
    @GetMapping("/unpaid")
    public ResponseEntity<List<InvoiceResponse>> getAllUnpaidInvoices() {
        List<InvoiceResponse> responses = invoiceService.getAllUnpaidInvoices();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get invoices by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(@PathVariable String status) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get invoices by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByDateRange(startDate, endDate);
        return ResponseEntity.ok(responses);
    }
}
