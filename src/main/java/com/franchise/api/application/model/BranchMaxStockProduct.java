package com.franchise.api.application.model;

import java.util.UUID;

public record BranchMaxStockProduct(
        UUID branchId,
        String branchName,
        UUID productId,
        String productName,
        long stock
) {
}

