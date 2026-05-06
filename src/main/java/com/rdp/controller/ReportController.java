package com.rdp.controller;


import com.rdp.dto.ProductReportDto;
import com.rdp.model.Product;
import com.rdp.model.InventoryItem;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;


@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ProductRepository productRepo;
    private final InventoryItemRepository inventoryRepo;

    @PreAuthorize("hasAnyAuthority('admin', 'manager')")
    @GetMapping("/inventory")
    public List<ProductReportDto> inventoryReport(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepo.findAll().stream().filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), categoryId)).toList();
        } else {
            products = productRepo.findAll();
        }
        List<ProductReportDto> result = new ArrayList<>();
        for (Product p : products) {
            List<InventoryItem> items = inventoryRepo.findByProduct(p);
            String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
            if (items.isEmpty()) {
                // No inventory for this product, show as out of stock with price as null
                result.add(new ProductReportDto(
                    p.getProductId(),
                    p.getProductCode(),
                    p.getName(),
                    categoryName,
                    null,
                    0,
                    p.getMinStock(),
                    p.getMaxStock(),
                    true
                ));
            } else {
                for (InventoryItem item : items) {
                    int available = item.getStock() == null ? 0 : item.getStock();
                    boolean outOfStock = available == 0;
                    result.add(new ProductReportDto(
                        p.getProductId(),
                        p.getProductCode(),
                        p.getName(),
                        categoryName,
                        item.getPrice(),
                        available,
                        p.getMinStock(),
                        p.getMaxStock(),
                        outOfStock
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Get low stock products - products where inventory < minStock
     * Category is required for this endpoint
     */
    @PreAuthorize("hasAnyAuthority('admin', 'manager')")
    @GetMapping("/inventory/low-stock")
    public List<ProductReportDto> lowStockReport(@RequestParam(value = "categoryId") Long categoryId) {
        List<Product> products = productRepo.findAll().stream()
            .filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), categoryId))
            .toList();
        
        List<ProductReportDto> result = new ArrayList<>();
        for (Product p : products) {
            Integer minStock = p.getMinStock();
            if (minStock == null || minStock <= 0) {
                continue; // Skip products without minStock configured
            }
            
            List<InventoryItem> items = inventoryRepo.findByProduct(p);
            String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
            
            // Calculate total available inventory for this product
            int totalAvailable = items.stream()
                .mapToInt(item -> item.getStock() == null ? 0 : item.getStock())
                .sum();
            
            // Include if total inventory < minStock
            if (totalAvailable < minStock) {
                if (items.isEmpty()) {
                    result.add(new ProductReportDto(
                        p.getProductId(),
                        p.getProductCode(),
                        p.getName(),
                        categoryName,
                        null,
                        0,
                        p.getMinStock(),
                        p.getMaxStock(),
                        true
                    ));
                } else {
                    // Add with the first item's price or aggregate
                    InventoryItem firstItem = items.get(0);
                    result.add(new ProductReportDto(
                        p.getProductId(),
                        p.getProductCode(),
                        p.getName(),
                        categoryName,
                        firstItem.getPrice(),
                        totalAvailable,
                        p.getMinStock(),
                        p.getMaxStock(),
                        totalAvailable == 0
                    ));
                }
            }
        }
        return result;
    }

    /**
     * Get overstock products - products where inventory > maxStock
     * Category is required for this endpoint
     */
    @PreAuthorize("hasAnyAuthority('admin', 'manager')")
    @GetMapping("/inventory/overstock")
    public List<ProductReportDto> overstockReport(@RequestParam(value = "categoryId") Long categoryId) {
        List<Product> products = productRepo.findAll().stream()
            .filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), categoryId))
            .toList();
        
        List<ProductReportDto> result = new ArrayList<>();
        for (Product p : products) {
            Integer maxStock = p.getMaxStock();
            if (maxStock == null || maxStock <= 0) {
                continue; // Skip products without maxStock configured
            }
            
            List<InventoryItem> items = inventoryRepo.findByProduct(p);
            String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
            
            // Calculate total available inventory for this product
            int totalAvailable = items.stream()
                .mapToInt(item -> item.getStock() == null ? 0 : item.getStock())
                .sum();
            
            // Include if total inventory > maxStock
            if (totalAvailable > maxStock) {
                if (!items.isEmpty()) {
                    InventoryItem firstItem = items.get(0);
                    result.add(new ProductReportDto(
                        p.getProductId(),
                        p.getProductCode(),
                        p.getName(),
                        categoryName,
                        firstItem.getPrice(),
                        totalAvailable,
                        p.getMinStock(),
                        p.getMaxStock(),
                        false
                    ));
                }
            }
        }
        return result;
    }
}
