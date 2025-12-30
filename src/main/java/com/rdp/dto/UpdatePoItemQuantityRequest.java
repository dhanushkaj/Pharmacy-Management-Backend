package com.rdp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePoItemQuantityRequest(
        @NotNull @Min(1) Integer quantity
) {}