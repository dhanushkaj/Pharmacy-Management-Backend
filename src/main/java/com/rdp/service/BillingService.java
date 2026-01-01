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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingRepository billingRepo;
    private final BillingItemRepository billingItemRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final InventoryItemRepository inventoryItemRepo;
    private final StockMovementService stockMovementService;

    @Transactional
    public BillingResponse createBilling(BillingRequest request) {
        // Find customer
        Customer customer = customerRepo.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        // Validate products and stock availability
        // validateStockAvailability(request.items()); // Allow negative inventory

        // Calculate totals
        BigDecimal subtotal = calculateSubtotal(request.items());
        
        // Use discount from request, or customer's default discount
        BigDecimal discountPercentage = request.discountPercentage() != null 
                ? request.discountPercentage() 
                : (customer.getDiscountPercentage() != null ? customer.getDiscountPercentage() : BigDecimal.ZERO);

        // Create billing
        Billing billing = Billing.builder()
                .billingNumber(generateBillingNumber())
                .customer(customer)
                .billingDate(LocalDateTime.now())
                .subtotal(subtotal)
                .discountPercentage(discountPercentage)
                .paymentMethod(request.paymentMethod())
                .notes(request.notes())
                .build();

        // Add billing items
        for (BillingItemRequest itemReq : request.items()) {
            Product product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.productId()));

            BillingItem item = BillingItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .batchNo(itemReq.batchNo())
                    .build();

            billing.addItem(item);
        }

        // Save billing
        billing = billingRepo.save(billing);

        // Update inventory (reduce stock)
        updateInventoryForBilling(billing);

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

            // Find inventory items for this product
            List<InventoryItem> inventoryItems = inventoryItemRepo.findByProductProductIdOrderByCreatedAtDesc(product.getProductId());

            int remainingToReduce = quantityToReduce;
            for (InventoryItem invItem : inventoryItems) {
                if (remainingToReduce <= 0) break;

                int currentStock = invItem.getStock() != null ? invItem.getStock() : 0;
                int reduceAmount = Math.min(currentStock, remainingToReduce);

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
                moveReq.setPrice(invItem.getPrice());
                stockMovementService.createMovement(product.getProductId(), moveReq);

                remainingToReduce -= reduceAmount;

                log.debug("Reduced inventory: productId={} inventoryItemId={} reducedBy={} newStock={}",
                        product.getProductId(), invItem.getId(), reduceAmount, invItem.getStock());
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
        
        log.info("Marked billing as printed: billingId={} billingNumber={}", billing.getBillingId(), billing.getBillingNumber());
        
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
}
