package com.franchise.api.application.port.out;

import com.franchise.api.domain.model.Franchise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface FranchiseRepositoryPort {

    Mono<Franchise> save(Franchise franchise);

    Mono<Franchise> findById(UUID franchiseId);

    Flux<Franchise> findAll();

    Mono<Boolean> existsByName(String name);
}
