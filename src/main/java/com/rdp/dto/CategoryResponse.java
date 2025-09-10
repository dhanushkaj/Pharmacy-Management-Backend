package com.rdp.dto;


public record CategoryResponse(
        Long categoryId,
        String name,
        String description
) {}