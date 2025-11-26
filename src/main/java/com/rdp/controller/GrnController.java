package com.rdp.controller;

import com.rdp.dto.GrnDtos.*;
import com.rdp.service.GrnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grns")
@RequiredArgsConstructor
public class GrnController {

    private final GrnService grnService;

    @PostMapping
    public ResponseEntity<GrnResponse> createGrn(@Valid @RequestBody CreateGrnRequest request) {
        GrnResponse createdGrn = grnService.createGrn(request);
        return new ResponseEntity<>(createdGrn, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<GrnResponse>> getAllGrns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(grnService.getAllGrns(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrnResponse> getGrnById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(grnService.getGrnById(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<GrnResponse> approveGrn(@PathVariable("id") Long id, @Valid @RequestBody ApproveGrnRequest request) {
        GrnResponse approvedGrn = grnService.approveGrn(id, request.approvedUser());
        return ResponseEntity.ok(approvedGrn);
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<GrnResponse> rejectGrn(@PathVariable("id") Long id, @Valid @RequestBody RejectGrnRequest request) {
        GrnResponse rejectedGrn = grnService.rejectGrn(id, request.reason());
        return ResponseEntity.ok(rejectedGrn);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrn(@PathVariable("id") Long id) {
        grnService.deleteGrn(id);
        return ResponseEntity.noContent().build();
    }
}