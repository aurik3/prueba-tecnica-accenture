package com.franchise.api.infrastructure.mongo.repository;

import com.franchise.api.infrastructure.mongo.document.ProductDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductMongoRepository extends ReactiveCrudRepository<ProductDocument, String> {

    Mono<Boolean> existsByBranchIdAndName(String branchId, String name);

    Mono<ProductDocument> findFirstByBranchIdOrderByStockDesc(String branchId);

    Mono<ProductDocument> findByIdAndBranchId(String id, String branchId);

    Flux<ProductDocument> findByBranchId(String branchId);
}
