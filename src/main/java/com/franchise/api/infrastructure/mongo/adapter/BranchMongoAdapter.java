package com.franchise.api.infrastructure.mongo.adapter;

import com.franchise.api.application.port.out.BranchRepositoryPort;
import com.franchise.api.domain.model.Branch;
import com.franchise.api.infrastructure.mongo.document.BranchDocument;
import com.franchise.api.infrastructure.mongo.mapper.IdMapper;
import com.franchise.api.infrastructure.mongo.repository.BranchMongoRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class BranchMongoAdapter implements BranchRepositoryPort {

    private final BranchMongoRepository repository;

    public BranchMongoAdapter(BranchMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Branch> save(Branch branch) {
        BranchDocument doc = new BranchDocument(
                IdMapper.toString(branch.id()),
                IdMapper.toString(branch.franchiseId()),
                branch.name()
        );
        return repository.save(doc).map(this::toDomain);
    }

    @Override
    public Mono<Branch> findById(UUID branchId) {
        return repository.findById(IdMapper.toString(branchId)).map(this::toDomain);
    }

    @Override
    public Flux<Branch> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Flux<Branch> findByFranchiseId(UUID franchiseId) {
        return repository.findByFranchiseId(IdMapper.toString(franchiseId)).map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByFranchiseIdAndName(UUID franchiseId, String name) {
        return repository.existsByFranchiseIdAndName(IdMapper.toString(franchiseId), name);
    }

    private Branch toDomain(BranchDocument doc) {
        return new Branch(
                IdMapper.toUuid(doc.getId()),
                IdMapper.toUuid(doc.getFranchiseId()),
                doc.getName()
        );
    }
}
