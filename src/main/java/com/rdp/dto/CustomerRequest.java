package com.rdp.dto;

import jakarta.validation.constraints.*;

public record CustomerRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String phone,
        @Email @Size(max = 100) String email,
        String address
) {}