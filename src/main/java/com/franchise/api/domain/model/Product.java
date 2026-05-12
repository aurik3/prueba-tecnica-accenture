package com.franchise.api.domain.model;

import java.util.UUID;

public record Product(UUID id, UUID branchId, String name, long stock) {

    public static Product create(UUID id, UUID branchId, String name, Long stock) {
        return new Product(id, branchId, DomainValidators.normalizeName(name, "name"), DomainValidators.validateStock(stock));
    }

    public Product rename(String newName) {
        return new Product(id, branchId, DomainValidators.normalizeName(newName, "name"), stock);
    }

    public Product updateStock(Long newStock) {
        return new Product(id, branchId, name, DomainValidators.validateStock(newStock));
    }
}

