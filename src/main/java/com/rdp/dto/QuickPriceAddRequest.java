package com.rdp.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record QuickPriceAddRequest(
        @NotNull(message = "Price must not be null")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price
) {
    @JsonCreator
    public QuickPriceAddRequest(@JsonProperty("price") BigDecimal price) {
        this.price = price;
    }
}
