package com.franchise.api.application.service;

import com.franchise.api.application.port.out.BranchRepositoryPort;
import com.franchise.api.application.port.out.FranchiseRepositoryPort;
import com.franchise.api.application.port.out.ProductRepositoryPort;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.domain.model.Branch;
import com.franchise.api.domain.model.Franchise;
import com.franchise.api.domain.model.Product;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FranchiseQueryServiceTest {

    @Test
    void getMaxStockProductsByBranch_skipsBranchesWithoutProducts() {
        FranchiseRepositoryPort franchiseRepo = mock(FranchiseRepositoryPort.class);
        BranchRepositoryPort branchRepo = mock(BranchRepositoryPort.class);
        ProductRepositoryPort productRepo = mock(ProductRepositoryPort.class);

        UUID franchiseId = UUID.randomUUID();
        Branch branch1 = new Branch(UUID.randomUUID(), franchiseId, "Sucursal 1");
        Branch branch2 = new Branch(UUID.randomUUID(), franchiseId, "Sucursal 2");

        when(franchiseRepo.findById(franchiseId)).thenReturn(Mono.just(new Franchise(franchiseId, "F1")));
        when(branchRepo.findByFranchiseId(franchiseId)).thenReturn(Flux.just(branch1, branch2));
        when(productRepo.findTopByBranchIdOrderByStockDesc(branch1.id()))
                .thenReturn(Mono.just(new Product(UUID.randomUUID(), branch1.id(), "P1", 50)));
        when(productRepo.findTopByBranchIdOrderByStockDesc(branch2.id()))
                .thenReturn(Mono.empty());

        FranchiseQueryService service = new FranchiseQueryService(franchiseRepo, branchRepo, productRepo);

        StepVerifier.create(service.getMaxStockProductsByBranch(franchiseId))
                .assertNext(item -> {
                    assertEquals(branch1.id(), item.branchId());
                    assertEquals("Sucursal 1", item.branchName());
                    assertEquals(50, item.stock());
                })
                .verifyComplete();
    }

    @Test
    void getMaxStockProductsByBranch_whenFranchiseMissing_errors() {
        FranchiseRepositoryPort franchiseRepo = mock(FranchiseRepositoryPort.class);
        BranchRepositoryPort branchRepo = mock(BranchRepositoryPort.class);
        ProductRepositoryPort productRepo = mock(ProductRepositoryPort.class);

        when(franchiseRepo.findById(any())).thenReturn(Mono.empty());

        FranchiseQueryService service = new FranchiseQueryService(franchiseRepo, branchRepo, productRepo);

        StepVerifier.create(service.getMaxStockProductsByBranch(UUID.randomUUID()))
                .expectError(NotFoundException.class)
                .verify();
    }
}
