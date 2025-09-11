package com.rdp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank String name,
        String contact,
        @Email(message = "Invalid email") String email,
        String address
) {}