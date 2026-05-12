package com.franchise.api.application.port.out;

import com.franchise.api.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductRepositoryPort {

    Mono<Product> save(Product product);

    Mono<Product> findById(UUID productId);

    Mono<Product> findByIdAndBranchId(UUID productId, UUID branchId);

    Mono<Void> deleteById(UUID productId);

    Mono<Boolean> existsByBranchIdAndName(UUID branchId, String name);

    Mono<Product> findTopByBranchIdOrderByStockDesc(UUID branchId);

    Flux<Product> findByBranchId(UUID branchId);
}
