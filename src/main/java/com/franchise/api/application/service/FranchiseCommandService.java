package com.franchise.api.application.service;

import com.franchise.api.application.port.out.FranchiseRepositoryPort;
import com.franchise.api.domain.exception.ConflictException;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.domain.model.Franchise;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class FranchiseCommandService {

    private final FranchiseRepositoryPort franchiseRepository;

    public FranchiseCommandService(FranchiseRepositoryPort franchiseRepository) {
        this.franchiseRepository = franchiseRepository;
    }

    public Mono<Franchise> createFranchise(String name) {
        Franchise franchise = Franchise.create(UUID.randomUUID(), name);
        return franchiseRepository.existsByName(franchise.name())
                .flatMap(exists -> exists
                        ? Mono.error(new ConflictException("franchise name already exists"))
                        : franchiseRepository.save(franchise)
                );
    }

    public Mono<Franchise> renameFranchise(UUID franchiseId, String newName) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new NotFoundException("franchise not found")))
                .flatMap(existing -> {
                    Franchise renamed = existing.rename(newName);
                    if (renamed.name().equals(existing.name())) {
                        return Mono.just(existing);
                    }
                    return franchiseRepository.existsByName(renamed.name())
                            .flatMap(exists -> exists
                                    ? Mono.error(new ConflictException("franchise name already exists"))
                                    : franchiseRepository.save(renamed)
                            );
                });
    }
}

