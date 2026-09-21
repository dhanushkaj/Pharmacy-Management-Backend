package com.rdp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.dto.CreateMovementRequest;
import com.rdp.dto.SupplierReturnItemRequest;
import com.rdp.dto.SupplierReturnItemResponse;
import com.rdp.dto.SupplierReturnRequest;
import com.rdp.dto.SupplierReturnResponse;
import com.rdp.model.BinType;
import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import com.rdp.model.Supplier;
import com.rdp.model.SupplierReturn;
import com.rdp.model.SupplierReturnItem;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.SupplierRepository;
import com.rdp.repository.SupplierReturnItemRepository;
import com.rdp.repository.SupplierReturnRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierReturnService {

    private final SupplierReturnRepository returnRepo;
    private final SupplierReturnItemRepository itemRepo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;
    private final InventoryItemRepository inventoryRepo;
    private final StockMovementService stockMovementService;

    /**
     * Map SupplierReturnItem to response DTO
     */
    private SupplierReturnItemResponse itemToResponse(SupplierReturnItem item) {
        return new SupplierReturnItemResponse(
                item.getItemId(),
                item.getProduct().getProductId(),
                item.getProduct().getProductCode(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getItemTotal(),
                item.getBatchNo(),
                item.getNotes()
        );
    }

    /**
     * Map SupplierReturn to response DTO
     */
    private SupplierReturnResponse toResponse(SupplierReturn sr) {
        List<SupplierReturnItemResponse> items = sr.getReturnItems().stream()
                .map(this::itemToResponse)
                .toList();

        return new SupplierReturnResponse(
                sr.getReturnId(),
                sr.getReturnNumber(),
                sr.getReturnDate(),
                sr.getSupplier().getSupplierId(),
                sr.getSupplier().getName(),
                sr.getTotalReturnAmount(),
                items,
                sr.getNotes()
        );
    }

    /**
     * Get the currently authenticated username
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM"; // Fallback if no authentication found
    }

    /**
     * Generate unique return number
     */
    private String generateReturnNumber() {
        return "SR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Create stock movement record when inventory is reduced for supplier return
     */
    private void createSupplierReturnMovement(Long productId, String batchNo, Integer quantity, BigDecimal unitPrice, Long returnId) {
        CreateMovementRequest moveReq = new CreateMovementRequest();
        moveReq.setFromBin(BinType.INVENTORY);
        moveReq.setToBin(BinType.SUPPLIER_RETURN);
        moveReq.setQuantity(quantity);
        moveReq.setReferenceType("SUPPLIER_RETURN");
        moveReq.setReferenceId(String.valueOf(returnId)); // Store numeric ID as string for lookup
        moveReq.setPerformedBy(getCurrentUsername()); // Get current authenticated user
        moveReq.setBatchNo(batchNo);
        moveReq.setPrice(unitPrice);
        
        try {
            stockMovementService.createMovement(productId, moveReq);
        } catch (Exception e) {
            // Log but don't fail - movement recording shouldn't block the transaction
            System.err.println("Failed to create stock movement for supplier return: " + e.getMessage());
        }
    }

    /**
     * Reduce inventory stock for a returned product using multi-record allocation
     * 
     * Stock Allocation Strategy:
     * 1. Find the latest selling price (by updatedAt) - this is used to validate the unitPrice parameter
     * 2. Get all eligible inventory records for the product ordered by price recency
     * 3. Allocate the requested quantity across records:
     *    - Start with the latest price record (may have zero stock)
     *    - If insufficient stock in latest record, continue to next eligible record
     *    - Deduct from multiple records if needed
     * 4. Use the provided unitPrice (which must be the latest price) for transaction total
     * 
     * @param product Product being returned
     * @param quantity Quantity to deduct
     * @param unitPrice The latest selling price for the product (used for transaction total)
     * @param returnId The supplier return ID (for movement tracking)
     */
    private void reduceInventoryStock(Product product, Integer quantity, BigDecimal unitPrice, Long returnId) {
        // Step 1: Find the latest selling price to validate unitPrice parameter
        var latestPriceOpt = inventoryRepo.findLatestPriceByProductId(product.getProductId());
        if (latestPriceOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "No inventory records found for product " + product.getName()
            );
        }

        InventoryItem latestPriceRecord = latestPriceOpt.get();
        BigDecimal latestSellingPrice = latestPriceRecord.getPrice();

        // Validate that the provided unitPrice matches the latest selling price
        if (unitPrice.compareTo(latestSellingPrice) != 0) {
            throw new IllegalArgumentException(
                    "Product " + product.getName() + " has a newer selling price Rs. " + latestSellingPrice + 
                    ". Please refresh and try again."
            );
        }

        // Step 2: Get all eligible inventory records ordered by latest price first
        List<InventoryItem> allRecords = inventoryRepo.findByProductIdOrderByLatestPrice(product.getProductId());
        if (allRecords.isEmpty()) {
            throw new IllegalArgumentException(
                    "No inventory records found for product " + product.getName()
            );
        }

        // Step 3: Allocate quantity across records
        int remainingQuantity = quantity;
        for (InventoryItem record : allRecords) {
            if (remainingQuantity <= 0) {
                break;
            }

            int availableStock = record.getStock() != null ? record.getStock() : 0;
            if (availableStock <= 0) {
                continue; // Skip records with no stock
            }

            // Deduct what we can from this record
            int deduction = Math.min(remainingQuantity, availableStock);
            record.setStock(availableStock - deduction);
            inventoryRepo.save(record);
            remainingQuantity -= deduction;
            
            // Create stock movement for this deduction
            createSupplierReturnMovement(product.getProductId(), record.getBatchNo(), deduction, record.getPrice(), returnId);
        }

        // Step 4: Verify total available stock was sufficient
        if (remainingQuantity > 0) {
            // Calculate total available stock for error message
            int totalAvailable = allRecords.stream()
                    .mapToInt(r -> r.getStock() != null ? r.getStock() : 0)
                    .sum();
            
            throw new IllegalArgumentException(
                    "Insufficient stock for product " + product.getName() + 
                    ". Available: " + totalAvailable + ", Requested: " + quantity
            );
        }
    }

    /**
     * Create a new supplier return with multiple items
     */
    @Transactional
    public SupplierReturnResponse createReturn(SupplierReturnRequest request) {
        // Validate supplier exists
        Supplier supplier = supplierRepo.findById(request.supplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        // Validate return items
        if (request.returnItems() == null || request.returnItems().isEmpty()) {
            throw new IllegalArgumentException("Return must contain at least one item");
        }

        // Create supplier return without items first
        SupplierReturn supplierReturn = SupplierReturn.builder()
                .returnNumber(generateReturnNumber())
                .returnDate(LocalDateTime.now())
                .supplier(supplier)
                .notes(request.notes())
                .build();

        // Save return first to get the returnId (auto-generated)
        SupplierReturn saved = returnRepo.save(supplierReturn);
        Long returnId = saved.getReturnId();

        // Process each return item
        for (SupplierReturnItemRequest itemReq : request.returnItems()) {
            // Validate product exists
            Product product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.productId()));

            // Validate quantity
            if (itemReq.quantity() == null || itemReq.quantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for product: " + product.getName());
            }

            // Validate unit price
            if (itemReq.unitPrice() == null || itemReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Invalid unit price for product: " + product.getName());
            }

            // Reduce inventory stock for this return (using numeric returnId for referencing)
            reduceInventoryStock(product, itemReq.quantity(), itemReq.unitPrice(), returnId);

            // Create return item
            SupplierReturnItem item = SupplierReturnItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .batchNo(itemReq.batchNo())
                    .notes(itemReq.notes())
                    .build();

            saved.addReturnItem(item);
        }

        // Calculate total and save again with items
        saved.calculateTotalReturnAmount();
        SupplierReturn finalSaved = returnRepo.save(saved);

        return toResponse(finalSaved);
    }

    /**
     * Get all supplier returns (paginated)
     */
    public Page<SupplierReturnResponse> getAllReturns(Pageable pageable) {
        return returnRepo.findAll(pageable).map(this::toResponse);
    }

    /**
     * Get supplier returns for a specific supplier (paginated)
     */
    public Page<SupplierReturnResponse> getReturnsBySupplier(Long supplierId, Pageable pageable) {
        return returnRepo.findBySupplierPaginated(supplierId, pageable).map(this::toResponse);
    }

    /**
     * Get all supplier returns for a specific supplier
     */
    public List<SupplierReturnResponse> getReturnsBySupplierUnpaginated(Long supplierId) {
        return returnRepo.findBySupplier(supplierId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get single supplier return by ID
     */
    public SupplierReturnResponse getReturnById(Long returnId) {
        SupplierReturn sr = returnRepo.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier return not found"));
        return toResponse(sr);
    }

    /**
     * Get returns by date range
     */
    public List<SupplierReturnResponse> getReturnsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return returnRepo.findByDateRange(startDate, endDate).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Update an existing supplier return (replaces all items)
     */
    @Transactional
    public SupplierReturnResponse updateReturn(Long returnId, SupplierReturnRequest request) {
        SupplierReturn supplierReturn = returnRepo.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier return not found"));

        // Validate supplier
        if (!supplierReturn.getSupplier().getSupplierId().equals(request.supplierId())) {
            Supplier supplier = supplierRepo.findById(request.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
            supplierReturn.setSupplier(supplier);
        }

        // Clear existing items
        supplierReturn.getReturnItems().clear();

        // Add new items
        for (SupplierReturnItemRequest itemReq : request.returnItems()) {
            Product product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemReq.productId()));

            if (itemReq.quantity() == null || itemReq.quantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for product: " + product.getName());
            }

            if (itemReq.unitPrice() == null || itemReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Invalid unit price for product: " + product.getName());
            }

            SupplierReturnItem item = SupplierReturnItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .batchNo(itemReq.batchNo())
                    .notes(itemReq.notes())
                    .build();

            supplierReturn.addReturnItem(item);
        }

        supplierReturn.setNotes(request.notes());
        supplierReturn.calculateTotalReturnAmount();
        SupplierReturn updated = returnRepo.save(supplierReturn);

        return toResponse(updated);
    }

    /**
     * Delete a supplier return
     */
    @Transactional
    public void deleteReturn(Long returnId) {
        SupplierReturn sr = returnRepo.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier return not found"));
        returnRepo.delete(sr);
    }
}
