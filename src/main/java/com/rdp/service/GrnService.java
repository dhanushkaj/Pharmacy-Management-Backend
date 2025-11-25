package com.rdp.service;

import com.rdp.dto.GrnDtos.*;
import com.rdp.config.ResourceNotFoundException;
import com.rdp.model.*;
import com.rdp.model.enums.GrnStatus;
import com.rdp.repository.GrnRepository;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.PurchaseOrderRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrnService {

    private final GrnRepository grnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final InventoryItemRepository inventoryItemRepository;
    private static final Logger log = LoggerFactory.getLogger(GrnService.class);



    @Transactional
    public GrnResponse createGrn(CreateGrnRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.purchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found with id: " + request.purchaseOrderId()));

        Grn grn = Grn.builder()
                .purchaseOrder(po)
                .status(GrnStatus.PENDING)
                .grnCode(generateGrnCode()) // Implement this helper method
                .build();

        for (GrnItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.productId()));
            GrnItem grnItem = GrnItem.builder()
                    .product(product)
                    .receivedQuantity(itemRequest.receivedQuantity())
                    .unitCost(itemRequest.unitCost())
                    .build();
            grn.addItem(grnItem);
        }

        Grn savedGrn = grnRepository.save(grn);
        log.info("Created GRN id={} code={} purchaseOrderId={} items={}", savedGrn.getId(), savedGrn.getGrnCode(), po.getId(), savedGrn.getItems().size());
        return mapToDto(savedGrn);
    }


    @Transactional
    public GrnResponse approveGrn(Long grnId, String approvedByUser) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new ResourceNotFoundException("GRN not found with id: " + grnId));

        if (grn.getStatus() != GrnStatus.PENDING) {
            throw new IllegalStateException("Only PENDING GRNs can be approved. Current status: " + grn.getStatus());
        }

        grn.setStatus(GrnStatus.APPROVED);
        grn.setApprovedUser(approvedByUser);
        grn.setApprovedDate(LocalDateTime.now());

        int adjustedBuckets = 0;
        for (GrnItem item : grn.getItems()) {
            Product product = item.getProduct();
            BigDecimal newCost = item.getUnitCost();
            BigDecimal newSellPrice = newCost.multiply(BigDecimal.valueOf(1.2));

            // Check if inventory already has the same product + cost + price combination
            InventoryItem existingBucket = inventoryItemRepository
                    .findByProductAndCostPriceAndPrice(product, newCost, newSellPrice)
                    .orElse(null);

            if (existingBucket != null) {
                //Existing bucket found — increment stock
                existingBucket.setStock(existingBucket.getStock() + item.getReceivedQuantity());
                existingBucket.setUpdatedAt(LocalDateTime.now());
                inventoryItemRepository.save(existingBucket);
                adjustedBuckets++;
            } else {
                //No bucket for this cost/sell price — create new inventory record
                InventoryItem newBucket = InventoryItem.builder()
                        .product(product)
                        .costPrice(newCost)
                        .price(newSellPrice)
                        .stock(item.getReceivedQuantity())
                        .batchNo("BATCH-" + System.currentTimeMillis())
                        .build();

                inventoryItemRepository.save(newBucket);
                adjustedBuckets++;
            }
        }

        Grn updated = grnRepository.save(grn);
        log.info("Approved GRN id={} code={} items={} inventoryBucketsAdjusted={} approvedBy={}", grnId, grn.getGrnCode(), grn.getItems().size(), adjustedBuckets, approvedByUser);
        return mapToDto(updated);
    }




    @Transactional
    public GrnResponse rejectGrn(Long grnId, String reason) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new ResourceNotFoundException("GRN not found with id: " + grnId));

        if (grn.getStatus() != GrnStatus.PENDING) {
            throw new IllegalStateException("Only PENDING GRNs can be rejected. Current status: " + grn.getStatus());
        }

        grn.setStatus(GrnStatus.REJECTED);
        grn.setRejectedReason(reason);
        grn.setApprovedDate(null);
        grn.setApprovedUser(null);

        Grn updatedGrn = grnRepository.save(grn);
        log.info("Rejected GRN id={} code={} reason={} status={}", grnId, grn.getGrnCode(), reason, grn.getStatus());
        return mapToDto(updatedGrn);
    }

    public GrnResponse getGrnById(Long grnId) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new ResourceNotFoundException("GRN not found with id: " + grnId));
        log.debug("Fetched GRN id={} code={} status={}", grnId, grn.getGrnCode(), grn.getStatus());
        return mapToDto(grn);
    }

    public List<GrnResponse> getAllGrns() {
    List<GrnResponse> list = grnRepository.findAll().stream()
        .map(this::mapToDto)
        .collect(Collectors.toList());
    log.debug("Fetched all GRNs count={}", list.size());
    return list;
    }

    @Transactional
    public void deleteGrn(Long grnId) {
        Grn grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new ResourceNotFoundException("GRN not found with id: " + grnId));

        // Optional: Add logic to only allow deletion of PENDING or REJECTED GRNs
        if(grn.getStatus() == GrnStatus.APPROVED) {
            throw new IllegalStateException("Cannot delete an APPROVED GRN.");
        }

        grnRepository.delete(grn);
        log.info("Deleted GRN id={} code={} status={} items={}", grnId, grn.getGrnCode(), grn.getStatus(), grn.getItems().size());
    }

    // --- Helper Methods ---

    private String generateGrnCode() {
        // Simple implementation: GRN-YYYYMMDD-COUNT
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = grnRepository.count() + 1; // This is a simple but not concurrency-safe way
        String code = String.format("GRN-%s-%04d", datePart, count);
        log.debug("Generated GRN code {} (count={})", code, count);
        return code;
    }

    private GrnResponse mapToDto(Grn grn) {
        List<GrnItemResponse> itemResponses = grn.getItems().stream()
                .map(item -> new GrnItemResponse(
                        item.getId(),
                        item.getProduct().getProductId(),
                        item.getProduct().getName(),
                        item.getReceivedQuantity(),
                        item.getUnitCost()
                ))
                .collect(Collectors.toList());

        return new GrnResponse(
                grn.getId(),
                grn.getGrnCode(),
                grn.getPurchaseOrder().getId(),
                grn.getPurchaseOrder().getOrderCode(),
                grn.getCreatedAt(),
                grn.getApprovedDate(),
                grn.getApprovedUser(),
                grn.getStatus(),
                grn.getRejectedReason(),
                itemResponses
        );
    }
}