// com.rdp.service.InventoryService.java
package com.rdp.service;

import com.rdp.dto.CreateInventoryRequest;
import com.rdp.dto.UpdateInventoryRequest;
import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository inventoryRepo;
    private final ProductRepository productRepo;

    public boolean existsWithCost(Long productId, BigDecimal costPrice) {
        boolean exists = inventoryRepo.existsByProductProductIdAndCostPrice(productId, costPrice);
        log.debug("existsWithCost productId={} costPrice={} exists={}", productId, costPrice, exists);
        return exists;
    }

    @Transactional
    public InventoryItem addOrIncrement(Long productId, CreateInventoryRequest req) {
        Product p = productRepo.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        BigDecimal price = req.price();
        Integer qty = req.stock() == null ? 0 : req.stock();

        if (price != null) {
            var opt = inventoryRepo.findByProductAndPriceForUpdate(productId, price);
            if (opt.isPresent()) {
                InventoryItem existing = opt.get();
                int current = existing.getStock() == null ? 0 : existing.getStock();
                int added = (qty == null ? 0 : qty);
                existing.setStock(current + added);
                if (req.costPrice() != null) existing.setCostPrice(req.costPrice());
                inventoryRepo.save(existing);
                log.info("Incremented inventory bucket productId={} inventoryId={} price={} addedStock={} newStock={} costUpdated={}",
                        productId, existing.getId(), price, added, existing.getStock(), req.costPrice() != null);
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
    return saved;
    }

    /** Update an inventory bucket (absolute update of fields). */
    @Transactional
    public InventoryItem updateInventory(Long productId, Long inventoryId, UpdateInventoryRequest req) {
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
        return saved;
    }

    /** Delete an inventory bucket. */
    @Transactional
    public void deleteInventory(Long productId, Long inventoryId) {
        InventoryItem it = inventoryRepo.findById(inventoryId).orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + inventoryId));
        if (!it.getProduct().getProductId().equals(productId)) throw new IllegalArgumentException("Inventory does not belong to product");
        inventoryRepo.delete(it);
        log.info("Deleted inventory bucket inventoryId={} productId={} price={} stock={} batchNo={}",
                inventoryId, productId, it.getPrice(), it.getStock(), it.getBatchNo());
    }

    public List<InventoryItem> listByProduct(Long productId) {
        List<InventoryItem> list = inventoryRepo.findByProductProductIdOrderByCreatedAtDesc(productId);
        log.debug("List inventory buckets productId={} count={}", productId, list.size());
        return list;
    }
}
