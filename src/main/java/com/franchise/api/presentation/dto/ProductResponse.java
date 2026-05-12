package com.franchise.api.presentation.dto;

public record ProductResponse(
        String id,
        String branchId,
        String name,
        long stock
) {
}

