package com.franchise.api.infrastructure.mongo.adapter;

import com.franchise.api.application.port.out.FranchiseRepositoryPort;
import com.franchise.api.domain.model.Franchise;
import com.franchise.api.infrastructure.mongo.document.FranchiseDocument;
import com.franchise.api.infrastructure.mongo.mapper.IdMapper;
import com.franchise.api.infrastructure.mongo.repository.FranchiseMongoRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class FranchiseMongoAdapter implements FranchiseRepositoryPort {

    private final FranchiseMongoRepository repository;

    public FranchiseMongoAdapter(FranchiseMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        FranchiseDocument doc = new FranchiseDocument(IdMapper.toString(franchise.id()), franchise.name());
        return repository.save(doc).map(this::toDomain);
    }

    @Override
    public Mono<Franchise> findById(UUID franchiseId) {
        return repository.findById(IdMapper.toString(franchiseId)).map(this::toDomain);
    }

    @Override
    public Flux<Franchise> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByName(String name) {
        return repository.existsByName(name);
    }

    private Franchise toDomain(FranchiseDocument doc) {
        return new Franchise(IdMapper.toUuid(doc.getId()), doc.getName());
    }
}
