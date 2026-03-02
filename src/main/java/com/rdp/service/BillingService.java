package com.rdp.service;

import com.rdp.dto.BillingItemRequest;
import com.rdp.dto.BillingItemResponse;
import com.rdp.dto.BillingRequest;
import com.rdp.dto.BillingResponse;
import com.rdp.model.*;
import com.rdp.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {
    public List<com.rdp.dto.CustomerCreditReportDto> getCustomerCreditReport(String name, String phone, LocalDateTime startDate, LocalDateTime endDate) {
        List<Billing> billings = billingRepo.findAll().stream()
            .filter(b -> b.getPaymentMethod() == Billing.PaymentMethod.CREDIT)
            .filter(b -> !b.isPaid())
            .filter(b -> name == null || name.isEmpty() || b.getCustomer().getName().toLowerCase().contains(name.toLowerCase()))
            .filter(b -> phone == null || phone.isEmpty() || (b.getCustomer().getPhone() != null && b.getCustomer().getPhone().contains(phone)))
            .filter(b -> (startDate == null || !b.getBillingDate().isBefore(startDate)) && (endDate == null || !b.getBillingDate().isAfter(endDate)))
            .toList();
        return billings.stream().map(b -> com.rdp.dto.CustomerCreditReportDto.builder()
                .customerName(b.getCustomer().getName())
                .phone(b.getCustomer().getPhone())
                .billingNumber(b.getBillingNumber())
                .billingDate(b.getBillingDate())
                .grandTotal(b.getGrandTotal().doubleValue())
                .paid(b.isPaid())
                .build()).toList();
    }

    @Transactional
    public void markBillAsPaid(String billingNumber) {
        Billing bill = billingRepo.findByBillingNumber(billingNumber)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found: " + billingNumber));
        bill.setPaid(true);
        billingRepo.save(bill);
    }

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingRepository billingRepo;
    private final BillingItemRepository billingItemRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final InventoryItemRepository inventoryItemRepo;
    private final StockMovementService stockMovementService;
    private final AuditTrailService auditTrailService;

        @Transactional
        public void deleteBilling(Long billingId) {
                Billing billing = billingRepo.findById(billingId)
                                .orElseThrow(() -> new IllegalArgumentException("Billing not found: " + billingId));

                // Restore inventory and log stock movement for each billing item
                List<BillingItem> items = billingItemRepo.findByBillingBillingId(billingId);
                for (BillingItem item : items) {
                        Product product = item.getProduct();
                        // Find inventory item by product and price
                        inventoryItemRepo.findByProductAndPrice(product, item.getUnitPrice()).ifPresent(invItem -> {
                                int currentStock = invItem.getStock() != null ? invItem.getStock() : 0;
                                invItem.setStock(currentStock + item.getQuantity());
                                inventoryItemRepo.save(invItem);

                                // Log stock movement: SOLD -> INVENTORY
                                com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                                moveReq.setFromBin(com.rdp.model.BinType.SOLD);
                                moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                                moveReq.setQuantity(item.getQuantity());
                                moveReq.setReferenceType("BILLING_DELETE");
                                moveReq.setReferenceId(billing.getBillingNumber());
                                moveReq.setPerformedBy(billing.getCreatedBy());
                                moveReq.setBatchNo(item.getBatchNo());
                                moveReq.setPrice(item.getUnitPrice());
                                moveReq.setRemarks("Billing deleted, inventory returned");
                                stockMovementService.createMovement(product.getProductId(), moveReq);
                        });
                }

                billingRepo.delete(billing);
                log.info("Deleted billing and restored inventory: billingId={} billingNumber={}", billing.getBillingId(), billing.getBillingNumber());
        }

    @Transactional
    public BillingResponse createBilling(BillingRequest request) {
        Customer customer = customerRepo.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        // Calculate product-level and overall discount logic
        BigDecimal productLevelTotal = BigDecimal.ZERO;
        BigDecimal eligibleForOverall = BigDecimal.ZERO;
        List<BillingItem> items = new ArrayList<>();
        for (BillingItemRequest itemReq : request.items()) {
            Product product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.productId()));
            boolean exclude = Boolean.TRUE.equals(itemReq.excludeFromOverall());
            BigDecimal subtotal = itemReq.unitPrice().multiply(new BigDecimal(itemReq.quantity()));
            if (exclude) {
                productLevelTotal = productLevelTotal.add(subtotal);
            } else {
                eligibleForOverall = eligibleForOverall.add(subtotal);
            }
            BillingItem item = BillingItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .batchNo(itemReq.batchNo())
                    .subtotal(subtotal)
                    .build();
            items.add(item);
        }
        BigDecimal subtotal = productLevelTotal.add(eligibleForOverall);
        BigDecimal totalDiscount = request.totalDiscount() != null ? request.totalDiscount() : BigDecimal.ZERO;
        BigDecimal discountPercentage = request.discountPercentage() != null ? request.discountPercentage() : BigDecimal.ZERO;
        // Always use the discount amount from the request, do not recalculate from percentage
        BigDecimal grandTotal = subtotal.subtract(totalDiscount);

        Billing billing = Billing.builder()
            .billingNumber(generateBillingNumber())
            .customer(customer)
            .billingDate(LocalDateTime.now())
            .subtotal(subtotal)
            .discountAmount(totalDiscount) // always use the value sent from frontend
            .discountPercentage(discountPercentage)
            .grandTotal(grandTotal)
            .paymentMethod(request.paymentMethod())
            .notes(request.notes())
            .build();
        for (BillingItem item : items) {
            billing.addItem(item);
        }
        billing = billingRepo.save(billing);
        updateInventoryForBilling(billing);
        // Log discount application
        String userId = (customer.getCreatedBy() != null) ? customer.getCreatedBy() : "system";
        auditTrailService.logAction(
            userId,
            "DISCOUNT_APPLIED",
            "Billing created with discount",
            LocalDateTime.now(),
            null,
            request.totalDiscount() != null ? request.totalDiscount().toPlainString() : "0"
        );
        // Log payment type selection
        auditTrailService.logAction(
            userId,
            "PAYMENT_TYPE_SELECTED",
            "Payment type selected during billing",
            LocalDateTime.now(),
            null,
            request.paymentMethod() != null ? request.paymentMethod().name() : "N/A"
        );
        log.info("Created billing: billingId={} billingNumber={} customerId={} items={} grandTotal={}",
                billing.getBillingId(), billing.getBillingNumber(), customer.getCustomerId(),
                billing.getItems().size(), billing.getGrandTotal());
        return mapToResponse(billing);
    }

    private void validateStockAvailability(List<BillingItemRequest> items) {
        for (BillingItemRequest item : items) {
            Product product = productRepo.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

            // Get total stock from inventory items
            List<InventoryItem> inventoryItems = inventoryItemRepo.findByProductProductIdOrderByCreatedAtDesc(product.getProductId());
            int totalStock = inventoryItems.stream()
                    .mapToInt(inv -> inv.getStock() != null ? inv.getStock() : 0)
                    .sum();

            if (totalStock < item.quantity()) {
                throw new IllegalArgumentException(
                        String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                                product.getName(), totalStock, item.quantity())
                );
            }
        }
    }

    private BigDecimal calculateSubtotal(List<BillingItemRequest> items) {
        return items.stream()
                .map(item -> item.unitPrice().multiply(new BigDecimal(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateInventoryForBilling(Billing billing) {
        for (BillingItem item : billing.getItems()) {
            Product product = item.getProduct();
            int quantityToReduce = item.getQuantity();

            // FIFO: Find inventory items for this product, oldest first (createdAt ASC)
            List<InventoryItem> inventoryItems = inventoryItemRepo.findByProductProductIdOrderByCreatedAtAsc(product.getProductId());

            int remainingToReduce = quantityToReduce;
            for (InventoryItem invItem : inventoryItems) {
                if (remainingToReduce <= 0) break;

                int currentStock = invItem.getStock() != null ? invItem.getStock() : 0;
                int reduceAmount = Math.min(currentStock, remainingToReduce);

                if (reduceAmount > 0) {
                    invItem.setStock(currentStock - reduceAmount);
                    inventoryItemRepo.save(invItem);

                    // Create STOCK movement: INVENTORY -> SOLD
                    com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                    moveReq.setFromBin(com.rdp.model.BinType.INVENTORY);
                    moveReq.setToBin(com.rdp.model.BinType.SOLD);
                    moveReq.setQuantity(reduceAmount);
                    moveReq.setReferenceType("BILLING");
                    moveReq.setReferenceId(billing.getBillingNumber());
                    moveReq.setPerformedBy(billing.getCreatedBy());
                    moveReq.setBatchNo(invItem.getBatchNo());
                    moveReq.setPrice(invItem.getPrice()); // Inventory price for audit
                    stockMovementService.createMovement(product.getProductId(), moveReq);

                    log.debug("Reduced inventory: productId={} inventoryItemId={} reducedBy={} newStock={}",
                            product.getProductId(), invItem.getId(), reduceAmount, invItem.getStock());
                }
                remainingToReduce -= reduceAmount;
            }

            if (remainingToReduce > 0) {
                log.warn("Could not reduce full quantity from inventory: productId={} remaining={}",
                        product.getProductId(), remainingToReduce);
            }
        }
    }

    private String generateBillingNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long count = billingRepo.count() + 1;
        return String.format("BILL-%s-%04d", timestamp, count);
    }

    @Transactional(readOnly = true)
    public Page<BillingResponse> getAllBillings(Pageable pageable) {
        return billingRepo.findAllByOrderByBillingDateDesc(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BillingResponse getBillingById(Long billingId) {
        Billing billing = billingRepo.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found: " + billingId));
        return mapToResponse(billing);
    }

    @Transactional(readOnly = true)
    public Page<BillingResponse> getBillingsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return billingRepo.findByBillingDateBetween(startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BillingResponse> getBillingsByCustomer(Long customerId, Pageable pageable) {
        return billingRepo.findByCustomerCustomerIdOrderByBillingDateDesc(customerId)
                .stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::mapToResponse)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                ));
    }

    @Transactional
    public BillingResponse markAsPrinted(Long billingId) {
        Billing billing = billingRepo.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("Billing not found: " + billingId));
        billing.setIsPrinted(true);
        billing = billingRepo.save(billing);
        // Log bill reprint
        String userId = (billing.getCreatedBy() != null) ? billing.getCreatedBy() : "system";
        auditTrailService.logAction(
            userId,
            "BILL_REPRINTED",
            "Bill reprinted",
            LocalDateTime.now(),
            null,
            billing.getBillingNumber()
        );
        return mapToResponse(billing);
    }

    private BillingResponse mapToResponse(Billing billing) {
        Customer customer = billing.getCustomer();

        List<BillingItemResponse> itemResponses = billing.getItems().stream()
                .map(item -> new BillingItemResponse(
                        item.getBillingItemId(),
                        item.getProduct().getProductId(),
                        item.getProduct().getProductCode(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal(),
                        item.getBatchNo()
                ))
                .collect(Collectors.toList());

        return new BillingResponse(
                billing.getBillingId(),
                billing.getBillingNumber(),
                customer.getCustomerId(),
                customer.getName(),
                customer.getPhone(),
                customer.getAddress(),
                billing.getBillingDate(),
                billing.getSubtotal(),
                billing.getDiscountPercentage(),
                billing.getDiscountAmount(),
                billing.getGrandTotal(),
                billing.getPaymentMethod(),
                billing.getNotes(),
                billing.getIsPrinted(),
                itemResponses,
                billing.getCreatedAt(),
                billing.getCreatedBy()
        );
    }
    
    @Transactional(readOnly = true)
    public BillingResponse getBillingByNumber(String billingNumber) {
        var billing = billingRepo.findByBillingNumber(billingNumber).orElse(null);
        return billing != null ? mapToResponse(billing) : null;
    }
}
