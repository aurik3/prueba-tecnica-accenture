package com.franchise.api.infrastructure.mongo.adapter;

import com.franchise.api.application.port.out.ProductRepositoryPort;
import com.franchise.api.domain.model.Product;
import com.franchise.api.infrastructure.mongo.document.ProductDocument;
import com.franchise.api.infrastructure.mongo.mapper.IdMapper;
import com.franchise.api.infrastructure.mongo.repository.ProductMongoRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ProductMongoAdapter implements ProductRepositoryPort {

    private final ProductMongoRepository repository;

    public ProductMongoAdapter(ProductMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Product> save(Product product) {
        ProductDocument doc = new ProductDocument(
                IdMapper.toString(product.id()),
                IdMapper.toString(product.branchId()),
                product.name(),
                product.stock()
        );
        return repository.save(doc).map(this::toDomain);
    }

    @Override
    public Mono<Product> findById(UUID productId) {
        return repository.findById(IdMapper.toString(productId)).map(this::toDomain);
    }

    @Override
    public Mono<Product> findByIdAndBranchId(UUID productId, UUID branchId) {
        return repository.findByIdAndBranchId(IdMapper.toString(productId), IdMapper.toString(branchId)).map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(UUID productId) {
        return repository.deleteById(IdMapper.toString(productId));
    }

    @Override
    public Mono<Boolean> existsByBranchIdAndName(UUID branchId, String name) {
        return repository.existsByBranchIdAndName(IdMapper.toString(branchId), name);
    }

    @Override
    public Mono<Product> findTopByBranchIdOrderByStockDesc(UUID branchId) {
        return repository.findFirstByBranchIdOrderByStockDesc(IdMapper.toString(branchId)).map(this::toDomain);
    }

    @Override
    public Flux<Product> findByBranchId(UUID branchId) {
        return repository.findByBranchId(IdMapper.toString(branchId)).map(this::toDomain);
    }

    private Product toDomain(ProductDocument doc) {
        return new Product(
                IdMapper.toUuid(doc.getId()),
                IdMapper.toUuid(doc.getBranchId()),
                doc.getName(),
                doc.getStock()
        );
    }
}
