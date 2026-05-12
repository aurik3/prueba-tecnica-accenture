package com.franchise.api.application.service;

import com.franchise.api.application.port.out.BranchRepositoryPort;
import com.franchise.api.application.port.out.ProductRepositoryPort;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.domain.model.Product;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductCommandServiceTest {

    @Test
    void updateProductStock_whenNotFound_returnsNotFound() {
        ProductRepositoryPort productRepo = mock(ProductRepositoryPort.class);
        BranchRepositoryPort branchRepo = mock(BranchRepositoryPort.class);
        when(productRepo.findByIdAndBranchId(any(), any())).thenReturn(Mono.empty());

        ProductCommandService service = new ProductCommandService(branchRepo, productRepo);

        StepVerifier.create(service.updateProductStock(UUID.randomUUID(), UUID.randomUUID(), 10L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void updateProductStock_whenValid_updatesAndReturns() {
        ProductRepositoryPort productRepo = mock(ProductRepositoryPort.class);
        BranchRepositoryPort branchRepo = mock(BranchRepositoryPort.class);

        UUID productId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Product existing = new Product(productId, branchId, "Producto A", 5L);

        when(productRepo.findByIdAndBranchId(productId, branchId)).thenReturn(Mono.just(existing));
        when(productRepo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0, Product.class)));

        ProductCommandService service = new ProductCommandService(branchRepo, productRepo);

        StepVerifier.create(service.updateProductStock(branchId, productId, 20L))
                .assertNext(updated -> assertEquals(20L, updated.stock()))
                .verifyComplete();
    }
}
