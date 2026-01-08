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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final StockMovementService stockMovementService;
    private static final Logger log = LoggerFactory.getLogger(GrnService.class);



    @Transactional
    public GrnResponse createGrn(CreateGrnRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.purchaseOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder not found with id: " + request.purchaseOrderId()));

        Grn grn = Grn.builder()
            .purchaseOrder(po)
            .status(GrnStatus.PENDING)
            .grnCode(generateGrnCode(po))
            .paid(request.paid() != null ? request.paid() : false)
            .paymentDueDate(request.paymentDueDate())
            .paymentDueDays(request.paymentDueDays())
            .chequeDate(request.chequeDate())
            .build();

        for (GrnItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.productId()));
            GrnItem grnItem = GrnItem.builder()
                .product(product)
                .receivedQuantity(itemRequest.receivedQuantity())
                .unitCost(itemRequest.unitCost())
                .price(itemRequest.price())
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
            BigDecimal newSellPrice = item.getPrice();

            // Try to find existing inventory by product and price (unique constraint)
            InventoryItem existingBucket = inventoryItemRepository
                    .findByProductAndPrice(product, newSellPrice)
                    .orElse(null);

            if (existingBucket != null) {
                // Existing bucket found — increment stock and update cost if needed
                existingBucket.setStock((existingBucket.getStock() == null ? 0 : existingBucket.getStock()) + item.getReceivedQuantity());
                if (newCost != null) existingBucket.setCostPrice(newCost);
                existingBucket.setUpdatedAt(LocalDateTime.now());
                inventoryItemRepository.save(existingBucket);

                // Create STOCK movement: GRN -> INVENTORY
                com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                moveReq.setFromBin(com.rdp.model.BinType.GRN);
                moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                moveReq.setQuantity(item.getReceivedQuantity());
                moveReq.setReferenceType("GRN");
                moveReq.setReferenceId(grn.getGrnCode());
                moveReq.setPerformedBy(approvedByUser);
                moveReq.setBatchNo(existingBucket.getBatchNo());
                moveReq.setPrice(newSellPrice);
                stockMovementService.createMovement(product.getProductId(), moveReq);

                adjustedBuckets++;
            } else {
                // No bucket for this price — create new inventory record
                InventoryItem newBucket = InventoryItem.builder()
                        .product(product)
                        .costPrice(newCost)
                        .price(newSellPrice)
                        .stock(item.getReceivedQuantity())
                        .batchNo("BATCH-" + System.currentTimeMillis())
                        .build();

                inventoryItemRepository.save(newBucket);

                // Create STOCK movement: GRN -> INVENTORY (use new bucket batch)
                com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                moveReq.setFromBin(com.rdp.model.BinType.GRN);
                moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                moveReq.setQuantity(item.getReceivedQuantity());
                moveReq.setReferenceType("GRN");
                moveReq.setReferenceId(grn.getGrnCode());
                moveReq.setPerformedBy(approvedByUser);
                moveReq.setBatchNo(newBucket.getBatchNo());
                moveReq.setPrice(newSellPrice);
                stockMovementService.createMovement(product.getProductId(), moveReq);

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

    public Page<GrnResponse> getAllGrns(Pageable pageable) {
        Page<GrnResponse> page = grnRepository.findAll(pageable).map(this::mapToDto);
        log.debug("Fetched GRNs page={} size={} total={}", pageable.getPageNumber(), page.getNumberOfElements(), page.getTotalElements());
        return page;
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

        // Allow deletion of GRNs in any status (PENDING, REJECTED, or APPROVED)
        grnRepository.delete(grn);
        log.info("Deleted GRN id={} code={} status={} items={}", grnId, grn.getGrnCode(), grn.getStatus(), grn.getItems().size());
    }

    // --- Helper Methods ---

    private String generateGrnCode(PurchaseOrder po) {
        // Format: GRN-POCODE-YYYYMMDD-SEQUENCE
        // e.g. GRN-PO-ACME-20251126-0001-20251126-0001
        // Keep PO code as-is (with dashes)
        String poCode = po.getOrderCode() != null ? po.getOrderCode() : "PO" + po.getId();
        
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Count GRNs for this PO today
        String prefix = "GRN-" + poCode + "-" + datePart + "-";
        long count = grnRepository.countByGrnCodeStartingWith(prefix);
        long sequence = count + 1;
        
        String code = String.format("%s%04d", prefix, sequence);
        log.debug("Generated GRN code {} (sequence={})", code, sequence);
        return code;
    }

        private GrnResponse mapToDto(Grn grn) {
        List<GrnItemResponse> itemResponses = grn.getItems().stream()
            .map(item -> new GrnItemResponse(
                item.getId(),
                item.getProduct().getProductId(),
                item.getProduct().getName(),
                item.getReceivedQuantity(),
                item.getUnitCost(),
                item.getPrice()
            ))
            .collect(Collectors.toList());

        String supplierName = null;
        if (grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getSupplier() != null) {
            supplierName = grn.getPurchaseOrder().getSupplier().getName();
        }
        return new GrnResponse(
            grn.getId(),
            grn.getGrnCode(),
            grn.getPurchaseOrder().getId(),
            grn.getPurchaseOrder().getOrderCode(),
            supplierName,
            grn.getCreatedAt(),
            grn.getApprovedDate(),
            grn.getApprovedUser(),
            grn.getStatus(),
            grn.getRejectedReason(),
            grn.getPaid(),
            grn.getPaymentDueDate(),
            grn.getPaymentDueDays(),
            grn.getChequeDate(),
            itemResponses
        );
        }
}