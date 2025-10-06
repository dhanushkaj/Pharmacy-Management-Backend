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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryRepo;
    private final ProductRepository productRepo;

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
                existing.setStock(current + (qty == null ? 0 : qty));
                // update costPrice if provided (optionally)
                if (req.costPrice() != null) existing.setCostPrice(req.costPrice());
                inventoryRepo.save(existing);
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
        return inventoryRepo.save(it);
    }

    /** Update an inventory bucket (absolute update of fields). */
    @Transactional
    public InventoryItem updateInventory(Long productId, Long inventoryId, UpdateInventoryRequest req) {
        InventoryItem it = inventoryRepo.findById(inventoryId).orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + inventoryId));
        if (!it.getProduct().getProductId().equals(productId)) throw new IllegalArgumentException("Inventory does not belong to product");
        if (req.price() != null) it.setPrice(req.price());
        it.setCostPrice(req.costPrice());
        it.setStock(req.stock() == null ? 0 : req.stock());
        it.setBatchNo(req.batchNo());
        return inventoryRepo.save(it);
    }

    /** Delete an inventory bucket. */
    @Transactional
    public void deleteInventory(Long productId, Long inventoryId) {
        InventoryItem it = inventoryRepo.findById(inventoryId).orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + inventoryId));
        if (!it.getProduct().getProductId().equals(productId)) throw new IllegalArgumentException("Inventory does not belong to product");
        inventoryRepo.delete(it);
    }

    public List<InventoryItem> listByProduct(Long productId) {
        return inventoryRepo.findByProductProductIdOrderByCreatedAtDesc(productId);
    }
}
