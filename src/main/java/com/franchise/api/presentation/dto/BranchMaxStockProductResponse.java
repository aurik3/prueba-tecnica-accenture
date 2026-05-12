package com.franchise.api.presentation.dto;

public record BranchMaxStockProductResponse(
        String branchId,
        String branchName,
        ProductSummary product
) {
    public record ProductSummary(
            String id,
            String name,
            long stock
    ) {
    }
}

