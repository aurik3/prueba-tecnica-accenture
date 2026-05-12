package com.franchise.api.application.port.out;

import com.franchise.api.domain.model.Branch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BranchRepositoryPort {

    Mono<Branch> save(Branch branch);

    Mono<Branch> findById(UUID branchId);

    Flux<Branch> findAll();

    Flux<Branch> findByFranchiseId(UUID franchiseId);

    Mono<Boolean> existsByFranchiseIdAndName(UUID franchiseId, String name);
}

