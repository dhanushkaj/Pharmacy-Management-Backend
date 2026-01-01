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
}
