package com.franchise.api.application.service;

import com.franchise.api.application.port.out.BranchRepositoryPort;
import com.franchise.api.application.port.out.ProductRepositoryPort;
import com.franchise.api.domain.exception.ConflictException;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.domain.model.Product;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ProductCommandService {

    private final BranchRepositoryPort branchRepository;
    private final ProductRepositoryPort productRepository;

    public ProductCommandService(BranchRepositoryPort branchRepository, ProductRepositoryPort productRepository) {
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
    }

    public Mono<Product> addProductToBranch(UUID branchId, String name, Long stock) {
        Product candidate = Product.create(UUID.randomUUID(), branchId, name, stock);
        return branchRepository.findById(branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("branch not found")))
                .then(productRepository.existsByBranchIdAndName(branchId, candidate.name()))
                .flatMap(exists -> exists
                        ? Mono.error(new ConflictException("product name already exists for branch"))
                        : productRepository.save(candidate)
                );
    }

    public Mono<Void> deleteProductFromBranch(UUID branchId, UUID productId) {
        return productRepository.findByIdAndBranchId(productId, branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("product not found for branch")))
                .flatMap(existing -> productRepository.deleteById(existing.id()));
    }

    public Mono<Product> updateProductStock(UUID branchId, UUID productId, Long newStock) {
        return productRepository.findByIdAndBranchId(productId, branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("product not found for branch")))
                .flatMap(existing -> productRepository.save(existing.updateStock(newStock)));
    }

    public Mono<Product> renameProduct(UUID branchId, UUID productId, String newName) {
        return productRepository.findByIdAndBranchId(productId, branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("product not found for branch")))
                .flatMap(existing -> {
                    Product renamed = existing.rename(newName);
                    if (renamed.name().equals(existing.name())) {
                        return Mono.just(existing);
                    }
                    return productRepository.existsByBranchIdAndName(branchId, renamed.name())
                            .flatMap(exists -> exists
                                    ? Mono.error(new ConflictException("product name already exists for branch"))
                                    : productRepository.save(renamed)
                            );
                });
    }
}
