package com.rdp.dto;

public record CustomerResponse(
        Long customerId,
        String name,
        String phone,
        String email,
        String address
) {}