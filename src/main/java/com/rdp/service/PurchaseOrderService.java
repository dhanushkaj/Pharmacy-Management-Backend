// src/main/java/com/rdp/service/PurchaseOrderService.java
package com.rdp.service;

import com.rdp.dto.PurchaseOrderItemResponse;
import com.rdp.dto.PurchaseOrderRequest;
import com.rdp.dto.PurchaseOrderResponse;
import com.rdp.model.PurchaseOrder;
import com.rdp.model.PurchaseOrderItem;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.PurchaseOrderItemRepository;
import com.rdp.repository.PurchaseOrderRepository;
import com.rdp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;
    private final PurchaseOrderItemRepository itemRepo;
    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);

    // e.g. PO-20250923-0001
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd


    public List<PurchaseOrderResponse> findAll() {
        return orderRepo.findAll().stream().map(this::toResponse).toList();
    }

    public PurchaseOrderResponse findById(Long id) {
        var po = orderRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("PO lookup failed id={}", id);
                    return new IllegalArgumentException("Purchase order not found: " + id);
                });
        log.debug("PO retrieved id={} code={} supplierId={}", id, po.getOrderCode(), po.getSupplier().getSupplierId());
        return toResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest req) {
        var supplier = supplierRepo.findById(req.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + req.supplierId()));

        var po = new PurchaseOrder();
        po.setCreatedAt(LocalDateTime.now());
        po.setNeededDate(req.neededDate());
        po.setSupplier(supplier);
        po.setOrderCode(nextOrderCode()); // robust code generator

        // attach items (cascade from PurchaseOrder -> PurchaseOrderItem)
        req.items().forEach(it -> {
            var product = productRepo.findById(it.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + it.productId()));
            var poi = new PurchaseOrderItem();
            poi.setProduct(product);
            poi.setQuantity(Math.max(1, it.quantity()));
            // optional: set unitCost snapshot here if you have a price source
            po.addItem(poi);
        });

        recalcAndSetTotal(po);
        var saved = orderRepo.save(po);
        log.info("Created PO id={} code={} supplierId={} items={} totalCost={}", saved.getId(), saved.getOrderCode(), supplier.getSupplierId(), saved.getItems().size(), saved.getTotalCost());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse updateItemQuantity(Long orderId, Long itemId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be >= 1");

        // ensure the order exists (and optionally to verify ownership)
        orderRepo.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found: " + orderId));

        var item = itemRepo.findOneInOrder(itemId, orderId)
                .orElseThrow(() -> new NoSuchElementException("Item not found in this order: " + itemId));

        item.setQuantity(quantity);
        itemRepo.save(item);

        // reload order to reflect updated items
        var order = orderRepo.findById(orderId).orElseThrow();
        recalcAndSetTotal(order);
        orderRepo.save(order);

        log.info("Updated PO item quantity orderId={} itemId={} newQty={} totalCost={}", orderId, itemId, quantity, order.getTotalCost());
        return toResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse deleteItem(Long orderId, Long itemId) {
        // ensure the order exists first
        orderRepo.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found: " + orderId));

        // delete the item within this order (scoped delete)
        itemRepo.deleteOneInOrder(itemId, orderId);

        // reload order and recalc total
        var order = orderRepo.findById(orderId).orElseThrow();
        recalcAndSetTotal(order);
        orderRepo.save(order);

        log.info("Deleted PO item orderId={} itemId={} itemsRemaining={} totalCost={}", orderId, itemId, order.getItems().size(), order.getTotalCost());
        return toResponse(order);
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepo.existsById(id)) {
            throw new IllegalArgumentException("Purchase order not found: " + id);
        }
        orderRepo.deleteById(id);
        log.info("Deleted PO id={}", id);
    }

    private String nextOrderCode() {
        String prefix = "PO-" + LocalDate.now().format(DAY) + "-"; // e.g. PO-20250923-
        int next = 1;

        // Requires this repository method:
        // Optional<PurchaseOrder> findTopByOrderCodeStartingWithOrderByOrderCodeDesc(String prefix);
        var lastOpt = orderRepo.findTopByOrderCodeStartingWithOrderByOrderCodeDesc(prefix);
        if (lastOpt.isPresent()) {
            String last = lastOpt.get().getOrderCode();   // e.g. PO-20250923-0042
            String suf = last.substring(prefix.length()); // "0042"
            try {
                next = Integer.parseInt(suf) + 1;
            } catch (NumberFormatException ignore) {
                next = 1; // fallback
            }
        }

        String code;
        int attempts = 0;
        do {
            code = prefix + String.format("%04d", next++);
            attempts++;
            if (attempts % 25 == 0) {
                log.debug("Attempted {} codes for prefix {} lastTried={}", attempts, prefix, code);
            }
            if (attempts > 1000) {
                throw new IllegalStateException("Could not allocate order code");
            }
        } while (orderRepo.existsByOrderCodeIgnoreCase(code));

        log.debug("Allocated order code {} after {} attempts", code, attempts);

        return code;
    }

    private void recalcAndSetTotal(PurchaseOrder order) {
        var total = order.getItems() == null ? BigDecimal.ZERO :
                order.getItems().stream()
                        .map(i -> safe(i.getUnitCost()).multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalCost(total);
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        var items = po.getItems() == null ? List.<PurchaseOrderItemResponse>of() :
                po.getItems().stream().map(i ->
                        new PurchaseOrderItemResponse(
                                i.getId(),
                                i.getProduct().getProductId(),
                                i.getProduct().getName(),
                                i.getProduct().getProductCode(),
                                i.getQuantity(),
                                i.getUnitCost()
                        )
                ).toList();

        return new PurchaseOrderResponse(
                po.getId(),
                po.getOrderCode(),
                po.getCreatedAt(),
                po.getNeededDate(),
                po.getSupplier().getSupplierId(),
                po.getSupplier().getName(),
                po.getTotalCost(),
                items
        );
    }
}
