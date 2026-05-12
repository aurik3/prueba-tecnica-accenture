package com.franchise.api.application.service;

import com.franchise.api.application.port.out.BranchRepositoryPort;
import com.franchise.api.application.port.out.FranchiseRepositoryPort;
import com.franchise.api.domain.exception.ConflictException;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.domain.model.Branch;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class BranchCommandService {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;

    public BranchCommandService(FranchiseRepositoryPort franchiseRepository, BranchRepositoryPort branchRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
    }

    public Mono<Branch> addBranchToFranchise(UUID franchiseId, String name) {
        Branch candidate = Branch.create(UUID.randomUUID(), franchiseId, name);
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new NotFoundException("franchise not found")))
                .then(branchRepository.existsByFranchiseIdAndName(franchiseId, candidate.name()))
                .flatMap(exists -> exists
                        ? Mono.error(new ConflictException("branch name already exists for franchise"))
                        : branchRepository.save(candidate)
                );
    }

    public Mono<Branch> renameBranch(UUID branchId, String newName) {
        return branchRepository.findById(branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("branch not found")))
                .flatMap(existing -> {
                    Branch renamed = existing.rename(newName);
                    if (renamed.name().equals(existing.name())) {
                        return Mono.just(existing);
                    }
                    return branchRepository.existsByFranchiseIdAndName(existing.franchiseId(), renamed.name())
                            .flatMap(exists -> exists
                                    ? Mono.error(new ConflictException("branch name already exists for franchise"))
                                    : branchRepository.save(renamed)
                            );
                });
    }
}

