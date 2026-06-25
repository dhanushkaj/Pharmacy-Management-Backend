package com.rdp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.dto.InventoryAuditDetailDto;
import com.rdp.dto.InventoryAuditDto;
import com.rdp.model.BinType;
import com.rdp.model.Category;
import com.rdp.model.InventoryAudit;
import com.rdp.model.InventoryAuditDetail;
import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import com.rdp.model.StockMovement;
import com.rdp.model.User;
import com.rdp.repository.CategoryRepository;
import com.rdp.repository.InventoryAuditDetailRepository;
import com.rdp.repository.InventoryAuditRepository;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.StockMovementRepository;
import com.rdp.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryAuditService {
    
    private final InventoryAuditRepository auditRepository;
    private final InventoryAuditDetailRepository auditDetailRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Export inventory for a category
     */
    @Transactional
    public InventoryAuditDto exportByCategory(Long categoryId, String notes, Long userId) {
        log.info("Starting inventory export for category: {}", categoryId);
        
        // Verify category and user
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        
        User exportedBy = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Get all products in category - use findAll and filter
        List<Product> allProducts = productRepository.findAll();
        List<Product> productsInCategory = allProducts.stream()
                .filter(p -> p.getCategory() != null && p.getCategory().getCategoryId().equals(categoryId))
                .collect(Collectors.toList());
        
        if (productsInCategory.isEmpty()) {
            throw new IllegalArgumentException("No products found in category: " + category.getName());
        }
        
        // Create audit record
        InventoryAudit audit = InventoryAudit.builder()
                .category(category)
                .exportedBy(exportedBy)
                .exportedAt(LocalDateTime.now())
                .status("EXPORTED")
                .notes(notes)
                .build();
        
        audit = auditRepository.save(audit);
        log.info("Created audit record: {}", audit.getAuditId());
        
        // Log file download event in stock movements for audit trail
        StockMovement downloadLog = StockMovement.builder()
                .productId(0L)  // 0 = system-level event, not product-specific
                .fromBin(BinType.INVENTORY)
                .toBin(BinType.INVENTORY)
                .quantity(0)
                .batchNo("AUDIT_DOWNLOAD")
                .price(java.math.BigDecimal.ZERO)
                .referenceType("AUDIT_FILE_EXPORT")
                .referenceId(String.valueOf(audit.getAuditId()))
                .performedBy(exportedBy.getUsername())
                .remarks("File downloaded: " + productsInCategory.size() + " products in " + category.getName())
                .build();
        stockMovementRepository.save(downloadLog);
        
        // Create detail records for each product
        List<InventoryAuditDetail> details = new ArrayList<>();
        
        for (Product product : productsInCategory) {
            // Get latest inventory level for this product
            List<InventoryItem> inventoryItems = inventoryItemRepository.findByProductProductIdOrderByCreatedAtDesc(product.getProductId());
            
            if (!inventoryItems.isEmpty()) {
                InventoryItem latestInventory = inventoryItems.get(0);
                
                InventoryAuditDetail detail = InventoryAuditDetail.builder()
                        .audit(audit)
                        .product(product)
                        .systemQtyAtExport(latestInventory.getStock())
                        .costPrice(latestInventory.getCostPrice())
                        .sellPrice(latestInventory.getPrice())
                        .variance(0) // Will be calculated when physical qty is entered
                        .build();
                
                details.add(detail);
            }
        }
        
        if (!details.isEmpty()) {
            auditDetailRepository.saveAll(details);
            log.info("Created {} audit detail records", details.size());
        }
        
        return convertToDto(audit, details);
    }

    /**
     * Get audit details
     */
    @Transactional(readOnly = true)
    public InventoryAuditDto getAudit(Long auditId) {
        InventoryAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        
        List<InventoryAuditDetail> details = auditDetailRepository.findByAuditId(auditId);
        
        return convertToDto(audit, details);
    }

    /**
     * Upload and process audit adjustments
     */
    @Transactional
    public Map<String, Object> uploadAdjustments(Long auditId, List<Map<String, Object>> auditData, Long uploadedBy) {
        log.info("Processing upload for audit: {}", auditId);
        
        InventoryAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        
        User uploader = userRepository.findById(uploadedBy)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + uploadedBy));
        
        int totalAdjustments = 0;
        int positivAdjustments = 0;
        int negativeAdjustments = 0;
        List<String> adjustmentLog = new ArrayList<>();
        
        // Process each item
        for (Map<String, Object> item : auditData) {
            Long productId = ((Number) item.get("productId")).longValue();
            Integer physicalQty = ((Number) item.get("physicalQty")).intValue();
            
            // Update audit detail
            List<InventoryAuditDetail> details = auditDetailRepository.findByAuditIdAndProductId(auditId, productId);
            if (!details.isEmpty()) {
                InventoryAuditDetail detail = details.get(0);
                detail.setPhysicalQty(physicalQty);
                detail.setVariance(physicalQty - detail.getSystemQtyAtExport());
                auditDetailRepository.save(detail);
                
                // If variance exists, adjust inventory
                if (detail.getVariance() != 0) {
                    totalAdjustments++;
                    
                    Product product = detail.getProduct();
                    BinType fromBin, toBin;
                    int quantityAdjustment = Math.abs(detail.getVariance());
                    
                    if (detail.getVariance() > 0) {
                        // Add stock
                        positivAdjustments++;
                        fromBin = BinType.GRN;
                        toBin = BinType.INVENTORY;
                        adjustmentLog.add("+ " + product.getName() + ": +" + quantityAdjustment);
                    } else {
                        // Remove stock
                        negativeAdjustments++;
                        fromBin = BinType.INVENTORY;
                        toBin = BinType.DAMAGED; // Use DAMAGED for stock reduction
                        adjustmentLog.add("- " + product.getName() + ": " + quantityAdjustment);
                    }
                    
                    // Create stock movement directly
                    StockMovement movement = StockMovement.builder()
                            .productId(productId)
                            .fromBin(fromBin)
                            .toBin(toBin)
                            .quantity(quantityAdjustment)
                            .batchNo("AUDIT_ADJUST")
                            .price(detail.getSellPrice())
                            .referenceType("INVENTORY_AUDIT")
                            .referenceId(String.valueOf(auditId))
                            .performedBy(uploader.getUsername())
                            .remarks("Physical inventory audit adjustment - " + (detail.getVariance() > 0 ? "Added" : "Removed") + " " + quantityAdjustment + " units")
                            .build();
                    
                    try {
                        stockMovementRepository.save(movement);
                        
                        // IMPORTANT: Update the actual InventoryItem quantities
                        List<InventoryItem> inventoryItems = inventoryItemRepository.findByProductProductIdOrderByCreatedAtDesc(productId);
                        if (!inventoryItems.isEmpty()) {
                            InventoryItem latestInventory = inventoryItems.get(0);
                            int newStock = latestInventory.getStock() + detail.getVariance();
                            latestInventory.setStock(newStock);
                            inventoryItemRepository.save(latestInventory);
                            log.info("Updated inventory for product: {}, new stock: {}", productId, newStock);
                        }
                        
                        log.info("Created stock movement for product: {}, variance: {}", productId, detail.getVariance());
                    } catch (Exception e) {
                        log.error("Failed to create stock movement for product: {}", productId, e);
                        throw new RuntimeException("Failed to adjust inventory for product: " + product.getName());
                    }
                }
            }
        }
        
        // Update audit status
        audit.setUploadedBy(uploader);
        audit.setUploadedAt(LocalDateTime.now());
        audit.setStatus("COMPLETED");
        auditRepository.save(audit);
        
        // Log file upload event in stock movements for audit trail
        StockMovement uploadLog = StockMovement.builder()
                .productId(0L)  // 0 = system-level event
                .fromBin(BinType.INVENTORY)
                .toBin(BinType.INVENTORY)
                .quantity(0)
                .batchNo("AUDIT_UPLOAD")
                .price(java.math.BigDecimal.ZERO)
                .referenceType("AUDIT_FILE_UPLOAD")
                .referenceId(String.valueOf(auditId))
                .performedBy(uploader.getUsername())
                .remarks("File uploaded: " + totalAdjustments + " adjustments (" + positivAdjustments + " added, " + negativeAdjustments + " removed)")
                .build();
        stockMovementRepository.save(uploadLog);
        
        log.info("Audit processing complete. Total adjustments: {}, Positive: {}, Negative: {}", 
                totalAdjustments, positivAdjustments, negativeAdjustments);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("auditId", auditId);
        response.put("totalAdjustments", totalAdjustments);
        response.put("positiveAdjustments", positivAdjustments);
        response.put("negativeAdjustments", negativeAdjustments);
        response.put("adjustmentLog", adjustmentLog);
        response.put("status", "COMPLETED");
        
        return response;
    }

    /**
     * Rollback audit adjustments - reverts both inventory and stock movements
     */
    @Transactional
    public Map<String, Object> rollback(Long auditId) {
        log.info("Rolling back audit: {}", auditId);
        
        InventoryAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));
        
        // Find all audit details to revert inventory
        List<InventoryAuditDetail> auditDetails = auditDetailRepository.findByAuditId(auditId);
        
        int reversionsCount = 0;
        List<String> reversals = new ArrayList<>();
        
        // Revert inventory quantities back to systemQtyAtExport
        for (InventoryAuditDetail detail : auditDetails) {
            if (detail.getVariance() != 0) {
                Long productId = detail.getProduct().getProductId();
                
                // Revert InventoryItem back to original system quantity
                List<InventoryItem> inventoryItems = inventoryItemRepository.findByProductProductIdOrderByCreatedAtDesc(productId);
                if (!inventoryItems.isEmpty()) {
                    InventoryItem latestInventory = inventoryItems.get(0);
                    int originalQty = detail.getSystemQtyAtExport();
                    latestInventory.setStock(originalQty);
                    inventoryItemRepository.save(latestInventory);
                    
                    reversionsCount++;
                    reversals.add("Reverted " + detail.getProduct().getName() + " to " + originalQty + " units");
                    log.info("Reverted product {} stock to original: {}", productId, originalQty);
                }
            }
        }
        
        // Find and create reversal stock movements for audit trail
        List<StockMovement> movements = stockMovementRepository.findByReferenceTypeAndReferenceId(
                "INVENTORY_AUDIT", 
                String.valueOf(auditId)
        );
        
        for (StockMovement movement : movements) {
            // Skip the download/upload log entries, only reverse actual adjustments
            if (movement.getProductId() > 0) {
                StockMovement reversal = StockMovement.builder()
                        .productId(movement.getProductId())
                        .fromBin(movement.getToBin())
                        .toBin(movement.getFromBin())
                        .quantity(movement.getQuantity())
                        .batchNo(movement.getBatchNo())
                        .price(movement.getPrice())
                        .referenceType("INVENTORY_AUDIT_ROLLBACK")
                        .referenceId(String.valueOf(auditId))
                        .performedBy("SYSTEM")
                        .remarks("Rollback of audit: " + auditId)
                        .build();
                
                stockMovementRepository.save(reversal);
                log.info("Created reversal movement for product: {}", movement.getProductId());
            }
        }
        
        // Update audit status
        audit.setStatus("ROLLED_BACK");
        auditRepository.save(audit);
        
        // Log rollback event
        StockMovement rollbackLog = StockMovement.builder()
                .productId(0L)
                .fromBin(BinType.INVENTORY)
                .toBin(BinType.INVENTORY)
                .quantity(0)
                .batchNo("AUDIT_ROLLBACK")
                .price(java.math.BigDecimal.ZERO)
                .referenceType("AUDIT_ROLLBACK")
                .referenceId(String.valueOf(auditId))
                .performedBy("SYSTEM")
                .remarks("Rollback completed: " + reversionsCount + " products reverted to original quantities")
                .build();
        stockMovementRepository.save(rollbackLog);
        
        log.info("Audit rollback complete. Reversions: {}", reversionsCount);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("auditId", auditId);
        response.put("reversionsCount", reversionsCount);
        response.put("reversals", reversals);
        response.put("status", "ROLLED_BACK");
        
        return response;
    }

    /**
     * Get audit history
     */
    @Transactional(readOnly = true)
    public List<InventoryAuditDto> getHistory(int limit) {
        return auditRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(InventoryAudit::getExportedAt).reversed())
                .limit(limit)
                .map(audit -> {
                    List<InventoryAuditDetail> details = auditDetailRepository.findByAuditId(audit.getAuditId());
                    return convertToDto(audit, details);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get variance report for an audit
     */
    @Transactional(readOnly = true)
    public List<InventoryAuditDetailDto> getVarianceReport(Long auditId) {
        return auditDetailRepository.findVariancesByAuditId(auditId)
                .stream()
                .map(this::convertDetailToDto)
                .collect(Collectors.toList());
    }

    /**
     * Helper: Convert audit to DTO
     */
    private InventoryAuditDto convertToDto(InventoryAudit audit, List<InventoryAuditDetail> details) {
        int itemsAdjusted = (int) details.stream().filter(d -> d.getVariance() != null && d.getVariance() != 0).count();
        
        return new InventoryAuditDto(
                audit.getAuditId(),
                audit.getCategory().getCategoryId(),
                audit.getCategory().getName(),
                audit.getExportedBy().getUserId(),
                audit.getExportedBy().getUsername(),
                audit.getExportedAt(),
                audit.getUploadedBy() != null ? audit.getUploadedBy().getUserId() : null,
                audit.getUploadedBy() != null ? audit.getUploadedBy().getUsername() : null,
                audit.getUploadedAt(),
                audit.getStatus(),
                audit.getNotes(),
                details.size(),
                itemsAdjusted,
                details.stream().map(this::convertDetailToDto).collect(Collectors.toList())
        );
    }

    /**
     * Helper: Convert detail to DTO
     */
    private InventoryAuditDetailDto convertDetailToDto(InventoryAuditDetail detail) {
        return new InventoryAuditDetailDto(
                detail.getAuditDetailId(),
                detail.getProduct().getProductId(),
                detail.getProduct().getName(),
                detail.getProduct().getProductCode(),
                detail.getSystemQtyAtExport(),
                detail.getPhysicalQty(),
                detail.getCostPrice(),
                detail.getSellPrice(),
                detail.getVariance(),
                detail.getNotes()
        );
    }
}
