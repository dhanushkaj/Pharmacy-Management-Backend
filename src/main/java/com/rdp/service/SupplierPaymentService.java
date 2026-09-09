package com.rdp.service;

import com.rdp.dto.SupplierPaymentRequest;
import com.rdp.dto.SupplierPaymentResponse;
import com.rdp.model.*;
import com.rdp.repository.SupplierPaymentRepository;
import com.rdp.repository.InvoiceRepository;
import com.rdp.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierPaymentService {

    private final SupplierPaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final SupplierRepository supplierRepository;

    public SupplierPaymentService(SupplierPaymentRepository paymentRepository,
                                 InvoiceRepository invoiceRepository,
                                 InvoiceService invoiceService,
                                 SupplierRepository supplierRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Record a payment for supplier invoice
     */
    @Transactional
    public SupplierPaymentResponse recordPayment(SupplierPaymentRequest request, String currentUser) {
        // Validate invoice exists
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Validate supplier
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        // Validate payment amount doesn't exceed remaining
        if (request.getPaymentAmount().compareTo(invoice.getAmountRemaining()) > 0) {
            throw new RuntimeException("Payment amount exceeds invoice remaining balance");
        }

        // Validate cheque date if payment method is CHECK
        if ("CHECK".equalsIgnoreCase(request.getPaymentMethod()) && request.getChequeDate() == null) {
            throw new RuntimeException("Cheque date is mandatory for cheque payments");
        }

        // Generate unique payment reference
        String paymentReference = generatePaymentReference();

        // Create payment record
        SupplierPayment payment = SupplierPayment.builder()
                .paymentReference(paymentReference)
                .invoice(invoice)
                .supplier(supplier)
                .paymentAmount(request.getPaymentAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .chequeDate(request.getChequeDate())
                .chequeNumber(request.getChequeNumber())
                .paymentStatus("COMPLETED")          // Payment recorded as completed
                .remarks(request.getRemarks())
                .createdBy(currentUser)
                .build();

        SupplierPayment saved = paymentRepository.save(payment);

        // Update invoice payment status
        invoiceService.updatePaymentStatus(request.getInvoiceId(), request.getPaymentAmount());

        return mapToResponse(saved);
    }

    /**
     * Get payment by ID
     */
    public SupplierPaymentResponse getPaymentById(Long paymentId) {
        SupplierPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return mapToResponse(payment);
    }

    /**
     * Get payment by reference
     */
    public SupplierPaymentResponse getPaymentByReference(String paymentReference) {
        SupplierPayment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return mapToResponse(payment);
    }

    /**
     * Get all payments
     */
    public List<SupplierPaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments for an invoice
     */
    public List<SupplierPaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findPaymentsByInvoice(invoiceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments for a supplier
     */
    public List<SupplierPaymentResponse> getPaymentsBySupplier(Long supplierId) {
        return paymentRepository.findPaymentsBySupplier(supplierId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get payments by status
     */
    public List<SupplierPaymentResponse> getPaymentsByStatus(String status) {
        return paymentRepository.findByPaymentStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get payments by date range (for Day-End Report, etc.)
     */
    public List<SupplierPaymentResponse> getPaymentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return paymentRepository.findPaymentsByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get total payments for a date range (useful for reconciliation)
     */
    public BigDecimal getTotalPaymentsByDateRange(LocalDate startDate, LocalDate endDate, String paymentMethod) {
        return paymentRepository.findPaymentsByDateRange(startDate, endDate)
                .stream()
                .filter(p -> paymentMethod == null || paymentMethod.equals(p.getPaymentMethod()))
                .map(SupplierPayment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Generate unique payment reference number
     * Format: PAY-SUPPLIER-YYYYMMDD-XXXX
     */
    private String generatePaymentReference() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePrefix = "PAY-SUPPLIER-" + now.format(dateFormatter);

        // Find max sequence for today
        Integer maxSeq = paymentRepository.findMaxPaymentSequence(datePrefix + "-%");
        int nextSeq = (maxSeq != null ? maxSeq : 0) + 1;

        return String.format("%s-%04d", datePrefix, nextSeq);
    }

    /**
     * Map SupplierPayment entity to response DTO
     */
    private SupplierPaymentResponse mapToResponse(SupplierPayment payment) {
        return SupplierPaymentResponse.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .invoiceId(payment.getInvoice().getId())
                .invoiceNumber(payment.getInvoice().getInvoiceNumber())
                .supplierId(payment.getSupplier().getSupplierId())
                .supplierName(payment.getSupplier().getName())
                .paymentAmount(payment.getPaymentAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .chequeDate(payment.getChequeDate())
                .chequeNumber(payment.getChequeNumber())
                .paymentStatus(payment.getPaymentStatus())
                .remarks(payment.getRemarks())
                .createdBy(payment.getCreatedBy())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
