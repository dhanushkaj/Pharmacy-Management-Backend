package com.rdp.service;

import com.rdp.dto.InvoiceRequest;
import com.rdp.dto.InvoiceResponse;
import com.rdp.model.*;
import com.rdp.repository.InvoiceRepository;
import com.rdp.repository.GrnRepository;
import com.rdp.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final GrnRepository grnRepository;
    private final SupplierRepository supplierRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                         GrnRepository grnRepository,
                         SupplierRepository supplierRepository) {
        this.invoiceRepository = invoiceRepository;
        this.grnRepository = grnRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Create a new invoice from supplier
     */
    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        // Validate GRN exists (no need to check if approved - can record invoice anytime)
        Grn grn = grnRepository.findById(request.getGrnId())
                .orElseThrow(() -> new RuntimeException("GRN not found"));

        // Validate supplier exists
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        // Check if invoice already exists for this GRN
        if (invoiceRepository.findByGrnId(request.getGrnId()).isPresent()) {
            throw new RuntimeException("Invoice already exists for this GRN");
        }

        // Create invoice
        Invoice invoice = Invoice.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .grn(grn)
                .supplier(supplier)
                .invoiceDate(request.getInvoiceDate())
                .invoiceAmount(request.getInvoiceAmount())
                .paymentDueDate(request.getPaymentDueDate())
                .paymentStatus("UNPAID")
                .amountPaid(BigDecimal.ZERO)
                .amountRemaining(request.getInvoiceAmount())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        return mapToResponse(saved);
    }

    /**
     * Get invoice by ID
     */
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return mapToResponse(invoice);
    }

    /**
     * Get invoice by invoice number
     */
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return mapToResponse(invoice);
    }

    /**
     * Get invoice for a specific GRN
     */
    public InvoiceResponse getInvoiceByGrn(Long grnId) {
        Invoice invoice = invoiceRepository.findByGrnId(grnId)
                .orElseThrow(() -> new RuntimeException("No invoice found for this GRN"));
        return mapToResponse(invoice);
    }

    /**
     * Get all invoices for a supplier
     */
    public List<InvoiceResponse> getInvoicesBySupplier(Long supplierId) {
        return invoiceRepository.findBySupplier(supplierId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unpaid invoices for a supplier (for payment screen)
     */
    public List<InvoiceResponse> getUnpaidInvoicesBySupplier(Long supplierId) {
        return invoiceRepository.findUnpaidInvoicesBySupplier(supplierId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get ALL unpaid invoices across all suppliers (for payment list view)
     */
    public List<InvoiceResponse> getAllUnpaidInvoices() {
        return invoiceRepository.findAllUnpaidInvoices()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get invoices by payment status
     */
    public List<InvoiceResponse> getInvoicesByStatus(String status) {
        return invoiceRepository.findByPaymentStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get invoices by date range
     */
    public List<InvoiceResponse> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findInvoicesByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update invoice payment status (called after payment is recorded)
     */
    @Transactional
    public InvoiceResponse updatePaymentStatus(Long invoiceId, BigDecimal amountPaid) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Update amount paid and remaining
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(amountPaid);
        invoice.setAmountPaid(newAmountPaid);
        invoice.setAmountRemaining(invoice.getInvoiceAmount().subtract(newAmountPaid));

        // Update status
        if (invoice.getAmountRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setPaymentStatus("PAID");
        } else if (newAmountPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setPaymentStatus("PARTIAL");
        }

        Invoice updated = invoiceRepository.save(invoice);
        return mapToResponse(updated);
    }

    /**
     * Map Invoice entity to response DTO
     */
    private InvoiceResponse mapToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .grnId(invoice.getGrn().getId())
                .supplierId(invoice.getSupplier().getSupplierId())
                .supplierName(invoice.getSupplier().getName())
                .invoiceDate(invoice.getInvoiceDate())
                .invoiceAmount(invoice.getInvoiceAmount())
                .paymentDueDate(invoice.getPaymentDueDate())
                .paymentStatus(invoice.getPaymentStatus())
                .amountPaid(invoice.getAmountPaid())
                .amountRemaining(invoice.getAmountRemaining())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
