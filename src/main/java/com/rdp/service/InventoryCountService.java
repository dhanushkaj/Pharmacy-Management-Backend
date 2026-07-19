package com.rdp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.dto.CreateMovementRequest;
import com.rdp.dto.InventoryCountLineDTO;
import com.rdp.dto.InventoryCountSessionDTO;
import com.rdp.dto.UpdateCountLineRequest;
import com.rdp.model.BinType;
import com.rdp.model.Category;
import com.rdp.model.CountSessionStatus;
import com.rdp.model.InventoryCountLine;
import com.rdp.model.InventoryCountSession;
import com.rdp.model.InventoryItem;
import com.rdp.model.InventoryLedgerEntry;
import com.rdp.model.MovementReasonType;
import com.rdp.model.Product;
import com.rdp.model.User;
import com.rdp.repository.CategoryRepository;
import com.rdp.repository.InventoryCountLineRepository;
import com.rdp.repository.InventoryCountSessionRepository;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.InventoryLedgerEntryRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryCountService {
    
    private final InventoryCountSessionRepository sessionRepository;
    private final InventoryCountLineRepository lineRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryLedgerEntryRepository ledgerRepository;
    private final StockMovementService stockMovementService;
    
    /**
     * Create a new DRAFT session for a category
     * Pre-populates lines with current InventoryItems for that category
     * Frontend checks for existing DRAFT first via getDraftSessionByCategory()
     */
    public InventoryCountSessionDTO createSession(Long categoryId, Long userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        
        User clerk = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Get next version number for this category
        List<InventoryCountSession> previousSessions = sessionRepository.findByCategoryId(categoryId);
        Integer nextVersion = previousSessions.isEmpty() ? 1 : previousSessions.stream()
                .mapToInt(InventoryCountSession::getVersionNumber)
                .max()
                .orElse(0) + 1;
        
        // Create session
        InventoryCountSession session = InventoryCountSession.builder()
                .category(category)
                .versionNumber(nextVersion)
                .status(CountSessionStatus.DRAFT)
                .createdBy(clerk)
                .build();
        
        session = sessionRepository.save(session);
        
        // Pre-populate lines from InventoryItems for this category
        List<InventoryCountLine> lines = populateLinesFromInventory(session, category);
        session.setLines(lines);
        session = sessionRepository.save(session);
        
        log.info("Created inventory count session: sessionId={} categoryId={} versionNumber={} lineCount={}",
                session.getId(), categoryId, nextVersion, lines.size());
        
        return mapSessionToDTO(session);
    }
    
    /**
     * Populate session lines from current InventoryItems for the category
     */
    private List<InventoryCountLine> populateLinesFromInventory(InventoryCountSession session, Category category) {
        List<Product> products = productRepository.findByCategory(category);
        
        List<InventoryCountLine> lines = products.stream().flatMap(product ->
                inventoryItemRepository.findByProduct(product).stream().map(inventoryItem -> 
                        InventoryCountLine.builder()
                                .session(session)
                                .product(product)
                                .inventoryItem(inventoryItem)
                                .productCode(product.getProductCode())
                                .productName(product.getName())
                                .sellPrice(inventoryItem.getPrice())
                                .systemQtyAtCount(inventoryItem.getStock() != null ? inventoryItem.getStock() : 0)
                                .physicalQty(null)
                                .variance(null)
                                .counted(false)
                                .lineComment(null)
                                .build()
                )
        ).collect(Collectors.toList());
        
        return lineRepository.saveAll(lines);
    }
    
    /**
     * Get session with full details
     */
    @Transactional(readOnly = true)
    public InventoryCountSessionDTO getSession(Long sessionId) {
        InventoryCountSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        // Initialize lazy collections
        Hibernate.initialize(session.getLines());
        
        return mapSessionToDTO(session);
    }
    
    /**
     * Get existing DRAFT session for a category (if any)
     */
    @Transactional(readOnly = true)
    public InventoryCountSessionDTO getDraftSessionByCategory(Long categoryId) {
        InventoryCountSession session = sessionRepository.findDraftByCategoryId(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("No DRAFT session found for category: " + categoryId));
        
        // Initialize lazy collections
        Hibernate.initialize(session.getLines());
        
        return mapSessionToDTO(session);
    }
    
    /**
     * Get all SUBMITTED sessions waiting for manager approval
     */
    @Transactional(readOnly = true)
    public List<InventoryCountSessionDTO> getSubmittedSessions() {
        // Query for submitted sessions by filtering the repository result
        List<InventoryCountSession> sessions = sessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == CountSessionStatus.SUBMITTED)
                .collect(Collectors.toList());
        
        // Initialize lazy collections for each session
        sessions.forEach(session -> Hibernate.initialize(session.getLines()));
        
        return sessions.stream()
                .map(this::mapSessionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all inventory count sessions - complete history for reporting
     * Sorted by created date descending (most recent first)
     */
    @Transactional(readOnly = true)
    public List<InventoryCountSessionDTO> getAllSessions() {
        List<InventoryCountSession> sessions = sessionRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Sort by date desc
                .collect(Collectors.toList());
        
        // Initialize lazy collections for each session
        sessions.forEach(session -> Hibernate.initialize(session.getLines()));
        
        return sessions.stream()
                .map(this::mapSessionToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Update lines with physical quantities and comments
     */
    public void updateLines(Long sessionId, List<UpdateCountLineRequest> updates) {
        InventoryCountSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (!session.getStatus().equals(CountSessionStatus.DRAFT)) {
            throw new IllegalArgumentException("Can only update lines in DRAFT status");
        }
        
        for (UpdateCountLineRequest update : updates) {
            InventoryCountLine line = lineRepository.findById(update.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Line not found: " + update.getId()));
            
            if (!line.getSession().getId().equals(sessionId)) {
                throw new IllegalArgumentException("Line does not belong to this session");
            }
            
            // Update physical quantity and variance
            if (update.getPhysicalQty() != null && update.getPhysicalQty() >= 0) {
                line.setPhysicalQty(update.getPhysicalQty());
                line.setVariance(update.getPhysicalQty() - line.getSystemQtyAtCount());
                line.setCounted(true);  // Automatically mark as counted when qty is entered
            }
            
            // Update comments
            if (update.getLineComment() != null) {
                line.setLineComment(update.getLineComment());
            }
            
            // Allow explicit marking as counted (for "confirmed zero" scenario)
            if (update.getCounted() != null) {
                line.setCounted(update.getCounted());
            }
            
            lineRepository.save(line);
        }
        
        log.info("Updated {} lines in session {}", updates.size(), sessionId);
    }
    
    /**
     * Update session-level comment
     */
    public void updateSessionComment(Long sessionId, String comment) {
        InventoryCountSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        session.setOverallComment(comment);
        sessionRepository.save(session);
    }
    
    /**
     * Submit session for approval - validates all lines are counted
     */
    public InventoryCountSessionDTO submitSession(Long sessionId, Long userId) {
        InventoryCountSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (!session.getStatus().equals(CountSessionStatus.DRAFT)) {
            throw new IllegalArgumentException("Only DRAFT sessions can be submitted");
        }
        
        // Check all lines are counted
        List<InventoryCountLine> uncountedLines = lineRepository.findUncountedBySessionId(sessionId);
        if (!uncountedLines.isEmpty()) {
            throw new IllegalArgumentException("Cannot submit: " + uncountedLines.size() + " lines are not yet counted");
        }
        
        session.setStatus(CountSessionStatus.SUBMITTED);
        session.setSubmittedAt(LocalDateTime.now());
        session = sessionRepository.save(session);
        
        log.info("Submitted session for approval: sessionId={} submittedAt={}", sessionId, session.getSubmittedAt());
        
        return mapSessionToDTO(session);
    }
    
    /**
     * Approve session - updates inventory and creates audit trail
     */
    public InventoryCountSessionDTO approveSession(Long sessionId, Long managerId) {
        InventoryCountSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (!session.getStatus().equals(CountSessionStatus.SUBMITTED)) {
            throw new IllegalArgumentException("Only SUBMITTED sessions can be approved");
        }
        
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + managerId));
        
        // Get all lines with their inventory items
        List<InventoryCountLine> lines = lineRepository.findBySessionId(sessionId);
        Hibernate.initialize(lines);
        
        // Process each line: update inventory and create ledger entry
        for (InventoryCountLine line : lines) {
            if (line.getPhysicalQty() != null && line.getVariance() != null && line.getVariance() != 0) {
                processLineApproval(line, manager);
            }
        }
        
        // Finalize session
        session.setStatus(CountSessionStatus.APPROVED);
        session.setApprovedBy(manager);
        session.setApprovedAt(LocalDateTime.now());
        session = sessionRepository.save(session);
        
        log.info("Approved inventory count session: sessionId={} categoryId={} approvedBy={} approvedAt={}",
                sessionId, session.getCategory().getCategoryId(), manager.getUserId(), session.getApprovedAt());
        
        return mapSessionToDTO(session);
    }
    
    /**
     * Process a single line during approval
     * Updates InventoryItem stock and creates ledger + stock movement entries
     */
    private void processLineApproval(InventoryCountLine line, User approver) {
        InventoryItem inventoryItem = line.getInventoryItem();
        Product product = line.getProduct();
        int variance = line.getVariance();
        
        int oldQty = inventoryItem.getStock() != null ? inventoryItem.getStock() : 0;
        int newQty = line.getPhysicalQty();
        
        // Update inventory item
        inventoryItem.setStock(newQty);
        inventoryItemRepository.save(inventoryItem);
        
        // Create ledger entry
        InventoryLedgerEntry ledgerEntry = InventoryLedgerEntry.builder()
                .product(product)
                .changeQty(variance)
                .reasonType(MovementReasonType.PHYSICAL_COUNT_ADJUSTMENT)
                .referenceSessionId(line.getSession().getId())
                .systemQtyBefore(oldQty)
                .systemQtyAfter(newQty)
                .createdBy(approver)
                .build();
        ledgerRepository.save(ledgerEntry);
        
        // Create stock movement only if variance exists
        if (variance != 0) {
            CreateMovementRequest moveReq = new CreateMovementRequest();
            moveReq.setFromBin(BinType.INVENTORY);
            moveReq.setToBin(BinType.INVENTORY);
            moveReq.setQuantity(Math.abs(variance));
            moveReq.setReferenceType("INVENTORY_COUNT_SESSION");
            moveReq.setReferenceId(line.getSession().getId().toString());
            moveReq.setPerformedBy(approver.getUserId().toString());
            moveReq.setBatchNo(inventoryItem.getBatchNo());
            moveReq.setPrice(inventoryItem.getPrice());
            moveReq.setRemarks(String.format("Physical Count Reconciliation - %s (Category: %s) - Variance: %d units",
                    product.getName(), line.getSession().getCategory().getName(), variance));
            
            stockMovementService.createMovement(product.getProductId(), moveReq);
        }
        
        log.info("Processed line approval: productId={} inventoryItemId={} variance={} oldQty={} newQty={}",
                product.getProductId(), inventoryItem.getId(), variance, oldQty, newQty);
    }
    
    /**
     * Reject session - moves it back to DRAFT with rejection reason
     */
    public InventoryCountSessionDTO rejectSession(Long sessionId, String reason, Long managerId) {
        InventoryCountSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (!session.getStatus().equals(CountSessionStatus.SUBMITTED)) {
            throw new IllegalArgumentException("Only SUBMITTED sessions can be rejected");
        }
        
        session.setStatus(CountSessionStatus.REJECTED);
        session.setRejectedReason(reason);
        session = sessionRepository.save(session);
        
        log.info("Rejected inventory count session: sessionId={} reason={} by={}",
                sessionId, reason, managerId);
        
        return mapSessionToDTO(session);
    }
    
    /**
     * Delete a DRAFT session - only DRAFTs can be deleted
     */
    public void deleteDraftSession(Long sessionId) {
        InventoryCountSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (!session.getStatus().equals(CountSessionStatus.DRAFT)) {
            throw new IllegalArgumentException("Only DRAFT sessions can be deleted. Current status: " + session.getStatus());
        }
        
        // Delete all lines first (due to foreign key constraint)
        lineRepository.deleteAll(session.getLines());
        
        // Delete the session
        sessionRepository.delete(session);
        
        log.info("Deleted DRAFT inventory count session: sessionId={} categoryId={}",
                sessionId, session.getCategory().getCategoryId());
    }
    
    /**
     * Convert entity to DTO
     */
    private InventoryCountSessionDTO mapSessionToDTO(InventoryCountSession session) {
        Hibernate.initialize(session.getLines());
        
        List<InventoryCountLineDTO> lineDTOs = session.getLines().stream()
                .map(this::mapLineToDTO)
                .collect(Collectors.toList());
        
        return InventoryCountSessionDTO.builder()
                .id(session.getId())
                .categoryId(session.getCategory().getCategoryId())
                .categoryName(session.getCategory().getName())
                .versionNumber(session.getVersionNumber())
                .status(session.getStatus())
                .createdById(session.getCreatedBy().getUserId())
                .createdByName(session.getCreatedBy().getUsername())
                .createdAt(session.getCreatedAt())
                .submittedAt(session.getSubmittedAt())
                .approvedById(session.getApprovedBy() != null ? session.getApprovedBy().getUserId() : null)
                .approvedByName(session.getApprovedBy() != null ? session.getApprovedBy().getUsername() : null)
                .approvedAt(session.getApprovedAt())
                .rejectedReason(session.getRejectedReason())
                .overallComment(session.getOverallComment())
                .updatedAt(session.getUpdatedAt())
                .lines(lineDTOs)
                .build();
    }
    
    /**
     * Convert line entity to DTO
     */
    private InventoryCountLineDTO mapLineToDTO(InventoryCountLine line) {
        return InventoryCountLineDTO.builder()
                .id(line.getId())
                .sessionId(line.getSession().getId())
                .productId(line.getProduct().getProductId())
                .inventoryItemId(line.getInventoryItem().getId())
                .productCode(line.getProductCode())
                .productName(line.getProductName())
                .sellPrice(line.getSellPrice())
                .systemQtyAtCount(line.getSystemQtyAtCount())
                .physicalQty(line.getPhysicalQty())
                .variance(line.getVariance())
                .counted(line.getCounted())
                .lineComment(line.getLineComment())
                .createdAt(line.getCreatedAt())
                .build();
    }
}
