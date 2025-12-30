package com.rdp.dto;

public record SupplierResponse(
        Long supplierId,
        String name,
        String contact,
        String email,
        String address
) {}
