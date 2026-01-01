package com.rdp.service;

import com.rdp.dto.CreateMovementRequest;
import com.rdp.dto.StockMovementDto;
import com.rdp.model.BinType;
import com.rdp.model.InventoryItem;
import com.rdp.model.StockMovement;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.StockMovementRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockMovementService {

    private final StockMovementRepository repository;
    private final InventoryItemRepository inventoryItemRepository;

    public StockMovementService(StockMovementRepository repository,InventoryItemRepository inventoryItemRepository) {
        this.repository = repository;
        this.inventoryItemRepository = inventoryItemRepository;
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
        // Add others as needed
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

        // group by batchNo and compute running inventory for each group starting from zero and applying deltas
        Map<String, Integer> running = new HashMap<>();
        List<StockMovementDto> dtos = new ArrayList<>();

        for (StockMovement sm : all) {
            String batch = sm.getBatchNo() == null ? "" : sm.getBatchNo();

            int delta = 0;
            if (sm.getToBin() == BinType.INVENTORY) delta += sm.getQuantity();
            if (sm.getFromBin() == BinType.INVENTORY) delta -= sm.getQuantity();

            int prev = running.getOrDefault(batch, 0);
            int newBal = prev + delta;
            running.put(batch, newBal);

            StockMovementDto dto = toDto(sm);
            dto.setInventoryBalanceAfter(newBal);
            dtos.add(dto);
        }

        // reverse to show latest first
        Collections.reverse(dtos);

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
        return StockMovementDto.builder()
                .id(sm.getId())
                .productId(sm.getProductId())
                .batchNo(sm.getBatchNo())
                .fromBin(sm.getFromBin())
                .toBin(sm.getToBin())
                .quantity(sm.getQuantity())
                .referenceType(sm.getReferenceType())
                .referenceId(sm.getReferenceId())
                .performedBy(sm.getPerformedBy())
                .remarks(sm.getRemarks())
                .createdAt(sm.getCreatedAt() != null ? sm.getCreatedAt().atOffset(ZoneOffset.UTC) : null)
                .build();
    }
}
