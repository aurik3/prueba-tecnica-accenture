package com.franchise.api.application.service;

import com.franchise.api.application.model.BranchMaxStockProduct;
import com.franchise.api.application.port.out.BranchRepositoryPort;
import com.franchise.api.application.port.out.FranchiseRepositoryPort;
import com.franchise.api.application.port.out.ProductRepositoryPort;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.domain.model.Branch;
import com.franchise.api.domain.model.Franchise;
import com.franchise.api.domain.model.Product;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class FranchiseQueryService {

    private final FranchiseRepositoryPort franchiseRepository;
    private final BranchRepositoryPort branchRepository;
    private final ProductRepositoryPort productRepository;

    public FranchiseQueryService(FranchiseRepositoryPort franchiseRepository,
                                 BranchRepositoryPort branchRepository,
                                 ProductRepositoryPort productRepository) {
        this.franchiseRepository = franchiseRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
    }

    public Flux<BranchMaxStockProduct> getMaxStockProductsByBranch(UUID franchiseId) {
        Mono<Void> franchiseExists = franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new NotFoundException("franchise not found")))
                .then();

        return franchiseExists.thenMany(Flux.defer(() -> branchRepository.findByFranchiseId(franchiseId)
                .flatMapSequential(branch -> productRepository.findTopByBranchIdOrderByStockDesc(branch.id())
                        .map(product -> new BranchMaxStockProduct(
                                branch.id(),
                                branch.name(),
                                product.id(),
                                product.name(),
                                product.stock()
                        ))
                )));
    }

    public Mono<Franchise> getFranchiseById(UUID franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new NotFoundException("franchise not found")));
    }

    public Flux<Franchise> getAllFranchises() {
        return franchiseRepository.findAll();
    }

    public Flux<Branch> getBranchesByFranchise(UUID franchiseId) {
        return getFranchiseById(franchiseId).thenMany(branchRepository.findByFranchiseId(franchiseId));
    }

    public Mono<Branch> getBranchById(UUID branchId) {
        return branchRepository.findById(branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("branch not found")));
    }

    public Flux<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public Flux<Product> getProductsByBranch(UUID branchId) {
        return getBranchById(branchId).thenMany(productRepository.findByBranchId(branchId));
    }

    public Mono<Product> getProductByIdInBranch(UUID branchId, UUID productId) {
        return getBranchById(branchId)
                .then(productRepository.findByIdAndBranchId(productId, branchId))
                .switchIfEmpty(Mono.error(new NotFoundException("product not found for branch")));
    }
}
