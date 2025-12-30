package com.rdp.service;

import com.rdp.dto.StoreSettingsDto;
import com.rdp.model.StoreSettings;
import com.rdp.repository.StoreSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {

    private final StoreSettingsRepository repository;

    /**
     * Get store settings. Returns the singleton settings record.
     * If no settings exist, returns null (frontend should show setup form).
     */
    public StoreSettingsDto getSettings() {
        return repository.findFirstByOrderBySettingsIdAsc()
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Save or update store settings. Maintains singleton pattern.
     */
    @Transactional
    public StoreSettingsDto saveSettings(StoreSettingsDto dto) {
        StoreSettings settings = repository.findFirstByOrderBySettingsIdAsc()
                .orElse(new StoreSettings());

        settings.setStoreName(dto.storeName());
        settings.setAddress(dto.address());
        settings.setPhone(dto.phone());
        settings.setEmail(dto.email());
        settings.setTaxId(dto.taxId());
        settings.setLogo(dto.logo());

        settings = repository.save(settings);
        return toDto(settings);
    }

    private StoreSettingsDto toDto(StoreSettings entity) {
        return new StoreSettingsDto(
                entity.getSettingsId(),
                entity.getStoreName(),
                entity.getAddress(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getTaxId(),
                entity.getLogo()
        );
    }
}
