package com.rdp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.dto.CreateInventoryCountSessionRequest;
import com.rdp.dto.InventoryCountSessionDTO;
import com.rdp.dto.RejectSessionRequest;
import com.rdp.dto.UpdateInventoryCountLinesRequest;
import com.rdp.dto.UpdateSessionCommentRequest;
import com.rdp.model.User;
import com.rdp.repository.UserRepository;
import com.rdp.service.InventoryCountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inventory-count")
@RequiredArgsConstructor
@Slf4j
public class InventoryCountController {
    
    private final InventoryCountService inventoryCountService;
    private final UserRepository userRepository;
    
    /**
     * Extract user ID from authentication
     */
    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getUserId();
    }
    
    /**
     * Create new inventory count session for a category
     */
    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLERK')")
    public ResponseEntity<InventoryCountSessionDTO> createSession(
            @Valid @RequestBody CreateInventoryCountSessionRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        InventoryCountSessionDTO session = inventoryCountService.createSession(request.getCategoryId(), userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }
    
    /**
     * Get session with all details and lines
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLERK')")
    public ResponseEntity<InventoryCountSessionDTO> getSession(@PathVariable Long sessionId) {
        InventoryCountSessionDTO session = inventoryCountService.getSession(sessionId);
        return ResponseEntity.ok(session);
    }
    
    /**
     * Get existing DRAFT session for a category
     * Used to resume or reject an existing DRAFT session
     */
    @GetMapping("/sessions/category/{categoryId}/draft")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLERK')")
    public ResponseEntity<InventoryCountSessionDTO> getDraftSessionByCategory(@PathVariable Long categoryId) {
        InventoryCountSessionDTO session = inventoryCountService.getDraftSessionByCategory(categoryId);
        return ResponseEntity.ok(session);
    }

    /**
     * List all SUBMITTED sessions waiting for manager approval
     * Manager-only endpoint
     */
    @GetMapping("/sessions/submitted")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<InventoryCountSessionDTO>> getSubmittedSessions() {
        List<InventoryCountSessionDTO> sessions = inventoryCountService.getSubmittedSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * List all sessions - complete history for admin/manager
     * Shows all sessions across all statuses
     */
    @GetMapping("/sessions/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<InventoryCountSessionDTO>> getAllSessions() {
        List<InventoryCountSessionDTO> sessions = inventoryCountService.getAllSessions();
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Update multiple lines with physical quantities and comments
     * Auto-saves on every blur/change from frontend
     */
    @PutMapping("/sessions/{sessionId}/lines")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLERK')")
    public ResponseEntity<Void> updateLines(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateInventoryCountLinesRequest request) {
        
        inventoryCountService.updateLines(sessionId, request.getLines());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Update session-level comment
     */
    @PutMapping("/sessions/{sessionId}/comment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLERK')")
    public ResponseEntity<Void> updateSessionComment(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateSessionCommentRequest request) {
        
        inventoryCountService.updateSessionComment(sessionId, request.getOverallComment());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Submit session for approval
     * Validates all lines are counted before submission
     */
    @PostMapping("/sessions/{sessionId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLERK')")
    public ResponseEntity<InventoryCountSessionDTO> submitSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        InventoryCountSessionDTO session = inventoryCountService.submitSession(sessionId, userId);
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * Approve session - manager/admin only
     * Updates inventory and creates audit trail
     */
    @PostMapping("/sessions/{sessionId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<InventoryCountSessionDTO> approveSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        InventoryCountSessionDTO session = inventoryCountService.approveSession(sessionId, userId);
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * Reject session - manager/admin only
     * Moves back to DRAFT for clerk revision
     */
    @PostMapping("/sessions/{sessionId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<InventoryCountSessionDTO> rejectSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody RejectSessionRequest request,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        InventoryCountSessionDTO session = inventoryCountService.rejectSession(sessionId, request.getReason(), userId);
        
        return ResponseEntity.ok(session);
    }
    
    /**
     * Delete a DRAFT session - only DRAFTs can be deleted
     * Non-DRAFT sessions cannot be deleted for audit purposes
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CLERK')")
    public ResponseEntity<Void> deleteDraftSession(@PathVariable Long sessionId) {
        inventoryCountService.deleteDraftSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
