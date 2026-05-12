package com.franchise.api.infrastructure.mongo.repository;

import com.franchise.api.infrastructure.mongo.document.BranchDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BranchMongoRepository extends ReactiveCrudRepository<BranchDocument, String> {

    Flux<BranchDocument> findByFranchiseId(String franchiseId);

    Mono<Boolean> existsByFranchiseIdAndName(String franchiseId, String name);
}

