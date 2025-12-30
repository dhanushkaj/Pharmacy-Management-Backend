package com.rdp.controller;

import com.rdp.dto.StoreSettingsDto;
import com.rdp.service.StoreSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store-settings")
@RequiredArgsConstructor
public class StoreSettingsController {

    private final StoreSettingsService service;

    /**
     * Get store settings (public endpoint - needed for bill printing)
     */
    @GetMapping
    public ResponseEntity<StoreSettingsDto> getSettings() {
        StoreSettingsDto settings = service.getSettings();
        if (settings == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(settings);
    }

    /**
     * Save or update store settings (admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreSettingsDto> saveSettings(@Valid @RequestBody StoreSettingsDto dto) {
        StoreSettingsDto saved = service.saveSettings(dto);
        return ResponseEntity.ok(saved);
    }
}
