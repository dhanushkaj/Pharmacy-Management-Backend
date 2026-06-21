// com.rdp.service.InventoryService.java
package com.rdp.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.dto.CreateInventoryRequest;
import com.rdp.dto.UpdateInventoryRequest;
import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository inventoryRepo;
    private final ProductRepository productRepo;
    private final StockMovementService stockMovementService;

    public boolean existsWithCost(Long productId, BigDecimal costPrice) {
        boolean exists = inventoryRepo.existsByProductProductIdAndCostPrice(productId, costPrice);
        log.debug("existsWithCost productId={} costPrice={} exists={}", productId, costPrice, exists);
        return exists;
    }

    @Transactional
    public InventoryItem addOrIncrement(Long productId, CreateInventoryRequest req, String performedBy) {
        Product p = productRepo.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        BigDecimal price = req.price();
        Integer qty = req.stock() == null ? 0 : req.stock();

        if (price != null) {
            var opt = inventoryRepo.findByProductIdAndPriceForUpdateNative(productId, price);
            if (opt.isPresent()) {
                InventoryItem existing = opt.get();
                int current = existing.getStock() == null ? 0 : existing.getStock();
                int added = (qty == null ? 0 : qty);
                existing.setStock(current + added);
                if (req.costPrice() != null) existing.setCostPrice(req.costPrice());
                inventoryRepo.save(existing);
                log.info("Incremented inventory bucket productId={} inventoryId={} price={} addedStock={} newStock={} costUpdated={}",
                        productId, existing.getId(), price, added, existing.getStock(), req.costPrice() != null);
                // Log stock movement for manual add/increment
                if (added > 0) {
                    com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                    moveReq.setFromBin(com.rdp.model.BinType.INVENTORY);
                    moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                    moveReq.setQuantity(added);
                    moveReq.setReferenceType("PRODUCT_UPDATE");
                    moveReq.setReferenceId(String.valueOf(productId));
                    moveReq.setPerformedBy(performedBy != null ? performedBy : "SYSTEM");
                    moveReq.setBatchNo(existing.getBatchNo());
                    moveReq.setPrice(price);
                    stockMovementService.createMovement(productId, moveReq);
                }
                return existing;
            }
        }
        // create new bucket
        InventoryItem it = InventoryItem.builder()
                .product(p)
                .price(req.price())
                .costPrice(req.costPrice())
                .stock(req.stock() == null ? 0 : req.stock())
                .batchNo(req.batchNo())
                .build();
    InventoryItem saved = inventoryRepo.save(it);
    log.info("Created inventory bucket productId={} inventoryId={} price={} costPrice={} stock={}",
        productId, saved.getId(), saved.getPrice(), saved.getCostPrice(), saved.getStock());
    // Log stock movement for manual add
    if (saved.getStock() > 0) {
        com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
        moveReq.setFromBin(com.rdp.model.BinType.INVENTORY);
        moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
        moveReq.setQuantity(saved.getStock());
        moveReq.setReferenceType("PRODUCT_UPDATE");
        moveReq.setReferenceId(String.valueOf(productId));
        moveReq.setPerformedBy(performedBy != null ? performedBy : "SYSTEM");
        moveReq.setBatchNo(saved.getBatchNo());
        moveReq.setPrice(saved.getPrice());
        stockMovementService.createMovement(productId, moveReq);
    }
    return saved;
    }

    /** Update an inventory bucket (absolute update of fields). */
    @Transactional
    public InventoryItem updateInventory(Long productId, Long inventoryId, UpdateInventoryRequest req, String performedBy) {
        InventoryItem it = inventoryRepo.findById(inventoryId).orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + inventoryId));
        if (!it.getProduct().getProductId().equals(productId)) throw new IllegalArgumentException("Inventory does not belong to product");
        Integer oldStock = it.getStock();
        if (req.price() != null) it.setPrice(req.price());
        it.setCostPrice(req.costPrice());
        it.setStock(req.stock() == null ? 0 : req.stock());
        it.setBatchNo(req.batchNo());
        InventoryItem saved = inventoryRepo.save(it);
        log.info("Updated inventory bucket inventoryId={} productId={} oldStock={} newStock={} price={} costPrice={} batchNo={}",
                inventoryId, productId, oldStock, saved.getStock(), saved.getPrice(), saved.getCostPrice(), saved.getBatchNo());
        // Log stock movement for manual update
        int delta = (saved.getStock() == null ? 0 : saved.getStock()) - (oldStock == null ? 0 : oldStock);
        if (delta != 0) {
            com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
            moveReq.setFromBin(com.rdp.model.BinType.INVENTORY);
            moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
            moveReq.setQuantity(Math.abs(delta));
            moveReq.setReferenceType("PRODUCT_UPDATE");
            moveReq.setReferenceId(String.valueOf(productId));
            moveReq.setPerformedBy(performedBy != null ? performedBy : "SYSTEM");
            moveReq.setBatchNo(saved.getBatchNo());
            moveReq.setPrice(saved.getPrice());
            moveReq.setRemarks(delta > 0 ? "Stock increased" : "Stock decreased");
            stockMovementService.createMovement(productId, moveReq);
        }
        return saved;
    }

    /** Delete an inventory bucket. */
    @Transactional
    public void deleteInventory(Long productId, Long inventoryId, String performedBy) {
        InventoryItem it = inventoryRepo.findById(inventoryId).orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + inventoryId));
        if (!it.getProduct().getProductId().equals(productId)) throw new IllegalArgumentException("Inventory does not belong to product");
        inventoryRepo.delete(it);
        log.info("Deleted inventory bucket inventoryId={} productId={} price={} stock={} batchNo={}",
                inventoryId, productId, it.getPrice(), it.getStock(), it.getBatchNo());
        // Log stock movement for manual delete
        if (it.getStock() != null && it.getStock() > 0) {
            com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
            moveReq.setFromBin(com.rdp.model.BinType.INVENTORY);
            moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
            moveReq.setQuantity(it.getStock());
            moveReq.setReferenceType("PRODUCT_UPDATE");
            moveReq.setReferenceId(String.valueOf(productId));
            moveReq.setPerformedBy(performedBy != null ? performedBy : "SYSTEM");
            moveReq.setBatchNo(it.getBatchNo());
            moveReq.setPrice(it.getPrice());
            moveReq.setRemarks("Inventory bucket deleted");
            stockMovementService.createMovement(productId, moveReq);
        }
    }

    public List<InventoryItem> listByProduct(Long productId) {
        List<InventoryItem> list = inventoryRepo.findByProductProductIdOrderByCreatedAtDesc(productId);
        log.debug("List inventory buckets productId={} count={}", productId, list.size());
        return list;
    }

    @Transactional
    public InventoryItem quickPriceAdd(Long productId, java.math.BigDecimal newPrice) {
        // 1. Verify product exists
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // 2. Check if price already exists for this product
        var existing = inventoryRepo.findByProductAndPrice(product, newPrice);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Price " + newPrice + " already exists for product: " + product.getName());
        }

        // 3. Create new inventory item with zero stock
        var newInv = InventoryItem.builder()
                .product(product)
                .price(newPrice)
                .costPrice(null)
                .stock(0)
                .batchNo(null)  // Will be null or auto-generated by batch service if needed
                .build();
        newInv = inventoryRepo.save(newInv);
        log.info("Created new inventory price level productId={} price={} via quick-price-add", productId, newPrice);

        // 4. Create stock movement entry for audit trail (quantity=0, but records the action)
        var moveReq = new com.rdp.dto.CreateMovementRequest();
        moveReq.setFromBin(com.rdp.model.BinType.GRN);
        moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
        moveReq.setQuantity(0);
        moveReq.setReferenceType("QUICK_PRICE_ADD");
        moveReq.setReferenceId(String.valueOf(productId));
        moveReq.setPerformedBy("SYSTEM");
        moveReq.setBatchNo(null);
        moveReq.setRemarks("Manual price level addition via quick-price: " + newPrice);
        moveReq.setPrice(newPrice);
        stockMovementService.createMovement(productId, moveReq);

        return newInv;
    }
}
