package com.franchise.api.infrastructure.mongo.repository;

import com.franchise.api.infrastructure.mongo.document.FranchiseDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface FranchiseMongoRepository extends ReactiveCrudRepository<FranchiseDocument, String> {

    Mono<Boolean> existsByName(String name);
}

