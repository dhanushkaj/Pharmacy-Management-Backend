package com.rdp.service;

import com.rdp.dto.InventoryReturnRequest;
import com.rdp.dto.InventoryReturnResponse;
import com.rdp.model.InventoryItem;
import com.rdp.model.InventoryReturn;
import com.rdp.model.Product;
import com.rdp.model.Supplier;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.InventoryReturnRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryReturnService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReturnService.class);

    private final InventoryReturnRepository returnRepo;
    private final ProductRepository productRepo;
    private final SupplierRepository supplierRepo;
    private final InventoryItemRepository inventoryItemRepo;

    @Transactional
    public InventoryReturnResponse createReturn(InventoryReturnRequest request) {
        // Find product
        Product product = productRepo.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.productId()));

        // Find supplier if provided
        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepo.findById(request.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.supplierId()));
        }

        // Validate business rules
        validateReturnRequest(request, product);

        // Create inventory return record
        InventoryReturn inventoryReturn = InventoryReturn.builder()
                .product(product)
                .returnType(request.returnType())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .reason(request.reason())
                .batchNo(request.batchNo())
                .customerName(request.customerName())
                .supplier(supplier)
                .notes(request.notes())
                .build();

        inventoryReturn = returnRepo.save(inventoryReturn);

        // Update product stock based on return type
        updateProductStock(product, request.returnType(), request.quantity());

        log.info("Created inventory return: returnId={} productId={} type={} quantity={}",
                inventoryReturn.getReturnId(), product.getProductId(), request.returnType(),
                request.quantity());

        return mapToResponse(inventoryReturn);
    }

    private void validateReturnRequest(InventoryReturnRequest request, Product product) {
        // If returning TO_SUPPLIER, ensure we have enough stock
        if (request.returnType() == InventoryReturn.ReturnType.TO_SUPPLIER) {
            // Get total stock from all inventory items for this product
            List<InventoryItem> items = inventoryItemRepo.findByProductProductIdOrderByCreatedAtDesc(product.getProductId());
            int totalStock = items.stream()
                    .mapToInt(item -> item.getStock() != null ? item.getStock() : 0)
                    .sum();
            
            if (totalStock < request.quantity()) {
                throw new IllegalArgumentException(
                        String.format("Insufficient stock. Current: %d, Requested return: %d",
                                totalStock, request.quantity())
                );
            }
        }

        // If returning TO_SUPPLIER, supplier should be provided
        if (request.returnType() == InventoryReturn.ReturnType.TO_SUPPLIER && request.supplierId() == null) {
            throw new IllegalArgumentException("Supplier is required when returning to supplier");
        }

        // If returning FROM_CUSTOMER, customer name should be provided
        if (request.returnType() == InventoryReturn.ReturnType.FROM_CUSTOMER &&
                (request.customerName() == null || request.customerName().isBlank())) {
            throw new IllegalArgumentException("Customer name is required when receiving return from customer");
        }
    }

    private void updateProductStock(Product product, InventoryReturn.ReturnType returnType, Integer quantity) {
        // Find inventory item with the matching price or create/update the first one
        List<InventoryItem> items = inventoryItemRepo.findByProductProductIdOrderByCreatedAtDesc(product.getProductId());
        
        if (returnType == InventoryReturn.ReturnType.FROM_CUSTOMER) {
            // Customer returned product → Add to inventory
            if (items.isEmpty()) {
                // Create new inventory item if none exists
                InventoryItem newItem = InventoryItem.builder()
                        .product(product)
                        .stock(quantity)
                        .price(new java.math.BigDecimal("0.00"))
                        .build();
                inventoryItemRepo.save(newItem);
                log.info("Created new inventory item: productId={} addedStock={}", 
                        product.getProductId(), quantity);
            } else {
                // Add to first inventory item
                InventoryItem item = items.get(0);
                int currentStock = item.getStock() != null ? item.getStock() : 0;
                item.setStock(currentStock + quantity);
                inventoryItemRepo.save(item);
                log.info("Added stock: productId={} previousStock={} added={} newStock={}",
                        product.getProductId(), currentStock, quantity, item.getStock());
            }
        } else if (returnType == InventoryReturn.ReturnType.TO_SUPPLIER) {
            // Returning to supplier → Reduce from inventory
            int remainingToReduce = quantity;
            for (InventoryItem item : items) {
                if (remainingToReduce <= 0) break;
                
                int currentStock = item.getStock() != null ? item.getStock() : 0;
                int reduceAmount = Math.min(currentStock, remainingToReduce);
                item.setStock(currentStock - reduceAmount);
                inventoryItemRepo.save(item);
                remainingToReduce -= reduceAmount;
                
                log.info("Reduced stock: productId={} inventoryItemId={} previousStock={} reduced={} newStock={}",
                        product.getProductId(), item.getId(), currentStock, reduceAmount, item.getStock());
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<InventoryReturnResponse> getAllReturns(Pageable pageable) {
        return returnRepo.findAllByOrderByReturnDateDesc(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<InventoryReturnResponse> getReturnsByType(InventoryReturn.ReturnType returnType, Pageable pageable) {
        return returnRepo.findByReturnType(returnType, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public InventoryReturnResponse getReturnById(Long returnId) {
        InventoryReturn inventoryReturn = returnRepo.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory return not found: " + returnId));
        return mapToResponse(inventoryReturn);
    }

    private InventoryReturnResponse mapToResponse(InventoryReturn inventoryReturn) {
        Product product = inventoryReturn.getProduct();
        Supplier supplier = inventoryReturn.getSupplier();

        return new InventoryReturnResponse(
                inventoryReturn.getReturnId(),
                product.getProductId(),
                product.getProductCode(),
                product.getName(),
                inventoryReturn.getReturnType(),
                inventoryReturn.getQuantity(),
                inventoryReturn.getUnitPrice(),
                inventoryReturn.getTotalAmount(),
                inventoryReturn.getReason(),
                inventoryReturn.getBatchNo(),
                inventoryReturn.getReturnDate(),
                inventoryReturn.getCustomerName(),
                supplier != null ? supplier.getSupplierId() : null,
                supplier != null ? supplier.getName() : null,
                inventoryReturn.getNotes(),
                inventoryReturn.getCreatedAt(),
                inventoryReturn.getCreatedBy()
        );
    }
}
