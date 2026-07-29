package com.rdp.service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.rdp.dto.CreateMovementRequest;
import com.rdp.dto.StockMovementDto;
import com.rdp.model.BinType;
import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import com.rdp.model.StockMovement;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.StockMovementRepository;

import jakarta.transaction.Transactional;


@Service
public class StockMovementService {
    private static final Logger log = LoggerFactory.getLogger(StockMovementService.class);

    public List<StockMovementDto> findByReference(String referenceType, String referenceId) {
        List<StockMovement> movements = repository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
        return movements.stream().map(this::toDto).collect(Collectors.toList());
    }

    private final StockMovementRepository repository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;

    public StockMovementService(StockMovementRepository repository, InventoryItemRepository inventoryItemRepository, ProductRepository productRepository) {
        this.repository = repository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.productRepository = productRepository;
    }

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();
    static {
        // Represent allowed transitions as FROM->TO strings
        ALLOWED_TRANSITIONS.add("GRN->INVENTORY");
        ALLOWED_TRANSITIONS.add("INVENTORY->SOLD");
        ALLOWED_TRANSITIONS.add("INVENTORY->CUSTOMER_RETURN");
        ALLOWED_TRANSITIONS.add("CUSTOMER_RETURN->INVENTORY");
        ALLOWED_TRANSITIONS.add("INVENTORY->SUPPLIER_RETURN");
        ALLOWED_TRANSITIONS.add("INVENTORY->EXPIRED");
        ALLOWED_TRANSITIONS.add("INVENTORY->DAMAGED");
        ALLOWED_TRANSITIONS.add("SUPPLIER_RETURN->INVENTORY");
        // Allow inventory return on billing delete
        ALLOWED_TRANSITIONS.add("SOLD->INVENTORY");
        // Allow manual inventory adjustments
        ALLOWED_TRANSITIONS.add("INVENTORY->INVENTORY");
        // Allow physical count reconciliation
        ALLOWED_TRANSITIONS.add("PHYSICAL_COUNT->INVENTORY");
    }

    private boolean isTransitionAllowed(BinType from, BinType to) {
        return ALLOWED_TRANSITIONS.contains(from.name() + "->" + to.name());
    }

    @Transactional
    public StockMovementDto createMovement(Long productId, CreateMovementRequest req) {
        // validate
        if (!isTransitionAllowed(req.getFromBin(), req.getToBin())) {
            throw new IllegalArgumentException("Invalid BIN transition: " + req.getFromBin() + " -> " + req.getToBin());
        }
        // If movement decreases inventory, ensure enough balance (by price)
        if (req.getFromBin() == BinType.INVENTORY) {
        	 Optional<InventoryItem>  itemOps = inventoryItemRepository.findByProductIdAndPriceForUpdateNative(productId, req.getPrice());
        	// InventoryItem item = itemOps.get();
            //if (item==null || item.getStock() < req.getQuantity()) {
            //    throw new IllegalStateException("Insufficient inventory balance for price " + req.getPrice() + ". Current: " + item);
            //}
        }

        StockMovement sm = StockMovement.builder()
                .productId(productId)
                .batchNo(StringUtils.hasText(req.getBatchNo()) ? req.getBatchNo() : "")
                .fromBin(req.getFromBin())
                .toBin(req.getToBin())
                .quantity(req.getQuantity())
                .referenceType(req.getReferenceType())
                .referenceId(req.getReferenceId())
                .performedBy(req.getPerformedBy())
                .remarks(req.getRemarks())
                .price(req.getPrice())
                .build();

        StockMovement saved = repository.save(sm);

        StockMovementDto dto = toDto(saved);
        dto.setInventoryBalanceAfter(calculateInventoryAfter(saved));
        return dto;
    }

    public Page<StockMovementDto> getMovements(Long productId, Optional<String> batchNoOpt, Optional<Date> startDate,
                                               Optional<Date> endDate, Optional<List<String>> binTypes, Optional<String> referenceType,
                                               Optional<String> performedBy, int page, int size) {
        // Note: For large datasets this implementation fetches all matching movements and computes running balances in memory.
        // For production with heavy volumes consider a native query with window functions or materialized aggregates.
        Specification<StockMovement> spec = (root, query, cb) -> cb.equal(root.get("productId"), productId);

        if (batchNoOpt.isPresent()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("batchNo"), batchNoOpt.get()));
        }
        if (startDate.isPresent()) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.get().toInstant().atOffset(ZoneOffset.UTC)));
        }
        if (endDate.isPresent()) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate.get().toInstant().atOffset(ZoneOffset.UTC)));
        }
        if (referenceType.isPresent()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceType"), referenceType.get()));
        }
        if (performedBy.isPresent()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("performedBy"), performedBy.get()));
        }
        if (binTypes.isPresent() && !binTypes.get().isEmpty()) {
            List<BinType> bins = binTypes.get().stream().map(BinType::valueOf).collect(Collectors.toList());
            spec = spec.and((root, query, cb) -> cb.or(root.get("toBin").in(bins), root.get("fromBin").in(bins)));
        }

        List<StockMovement> all = repository.findAll(spec);
        // sort ascending for running balance calc
        all.sort(Comparator.comparing(sm -> sm.getCreatedAt()));


        // Compute running balance starting from current inventory and working backwards
        Map<String, Integer> endingBalance = new HashMap<>();
        List<StockMovementDto> dtos = new ArrayList<>();

        // Get current inventory for each batch (or just product if batch not used)
        for (StockMovement sm : all) {
            String batch = sm.getBatchNo() == null ? "" : sm.getBatchNo();
            if (!endingBalance.containsKey(batch)) {
                Integer curr = repository.getInventoryBalanceForProduct(sm.getProductId());
                endingBalance.put(batch, curr != null ? curr : 0);
            }
        }

        // Walk movements in reverse to compute running balance after each movement
        Map<String, Integer> running = new HashMap<>(endingBalance);
        ListIterator<StockMovement> it = all.listIterator(all.size());
        List<StockMovementDto> reverseDtos = new ArrayList<>();
        while (it.hasPrevious()) {
            StockMovement sm = it.previous();
            String batch = sm.getBatchNo() == null ? "" : sm.getBatchNo();
            int after = running.getOrDefault(batch, 0);
            int delta = 0;
            if (sm.getToBin() == BinType.INVENTORY) delta -= sm.getQuantity();
            if (sm.getFromBin() == BinType.INVENTORY) delta += sm.getQuantity();
            int before = after + delta;
            running.put(batch, before);
            StockMovementDto dto = toDto(sm);
            dto.setInventoryBalanceAfter(after);
            reverseDtos.add(dto);
        }
        // Reverse to restore chronological order
        Collections.reverse(reverseDtos);
        dtos.addAll(reverseDtos);

        // (Removed extra reverse to preserve correct running balance order)

        // pagination
        int total = dtos.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<StockMovementDto> pageContent = dtos.subList(from, to);

        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(pageContent, pageable, total);
    }

    public Integer calculateInventoryAfter(StockMovement sm) {
        Integer current = repository.getInventoryBalanceForProduct(sm.getProductId());
        if (current == null) current = 0;
        // This returns current inventory; but inventoryAfter for the saved row should be computed by adding delta to prior inventory at that time.
        // For simplicity, return current (approx). Implement precise calculation by querying ordered movements when needed.
        return current;
    }

    private StockMovementDto toDto(StockMovement sm) {
        String productName = null;
        String productCode = null;
        try {
            var invItems = inventoryItemRepository.findByProductProductIdOrderByCreatedAtDesc(sm.getProductId());
            if (invItems != null && !invItems.isEmpty() && invItems.get(0).getProduct() != null) {
                productName = invItems.get(0).getProduct().getName();
                productCode = invItems.get(0).getProduct().getProductCode();
            } else {
                // fallback: fetch directly from Product
                Product product = productRepository.findById(sm.getProductId()).orElse(null);
                if (product != null) {
                    productName = product.getName();
                    productCode = product.getProductCode();
                }
            }
        } catch (Exception e) {
            // fallback: leave as null
        }
        if ("PRODUCT_UPDATE".equals(sm.getReferenceType())) {
            log.info("Manual Inventory Change DTO: id={}, productId={}, productName={}, productCode={}, batchNo={}, fromBin={}, toBin={}, quantity={}, performedBy={}, createdAt={}",
                sm.getId(), sm.getProductId(), productName, productCode, sm.getBatchNo(), sm.getFromBin(), sm.getToBin(), sm.getQuantity(), sm.getPerformedBy(), sm.getCreatedAt());
        }
        return StockMovementDto.builder()
                .id(sm.getId())
                .productId(sm.getProductId())
                .productName(productName)
                .productCode(productCode)
                .batchNo(sm.getBatchNo())
                .fromBin(sm.getFromBin())
                .toBin(sm.getToBin())
                .quantity(sm.getQuantity())
                .price(sm.getPrice())
                .referenceType(sm.getReferenceType())
                .referenceId(sm.getReferenceId())
                .performedBy(sm.getPerformedBy())
                .remarks(sm.getRemarks())
                .createdAt(sm.getCreatedAt() != null ? sm.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .build();
    }
}
