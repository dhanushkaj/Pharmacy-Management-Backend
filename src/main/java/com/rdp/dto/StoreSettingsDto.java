package com.rdp.dto;

import jakarta.validation.constraints.NotBlank;

public record StoreSettingsDto(
        Long settingsId,
        @NotBlank(message = "Store name is required")
        String storeName,
        @NotBlank(message = "Address is required")
        String address,
        @NotBlank(message = "Phone is required")
        String phone,
        String email,
        String taxId,
        String logo
) {
}
