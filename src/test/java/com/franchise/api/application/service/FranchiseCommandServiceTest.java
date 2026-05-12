package com.franchise.api.application.service;

import com.franchise.api.application.port.out.FranchiseRepositoryPort;
import com.franchise.api.domain.exception.ConflictException;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.domain.model.Franchise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranchiseCommandServiceTest {

    @Test
    void createFranchise_whenNameExists_returnsConflict() {
        FranchiseRepositoryPort repo = mock(FranchiseRepositoryPort.class);
        when(repo.existsByName("Franquicia A")).thenReturn(Mono.just(true));

        FranchiseCommandService service = new FranchiseCommandService(repo);

        StepVerifier.create(service.createFranchise("Franquicia A"))
                .expectError(ConflictException.class)
                .verify();
    }

    @Test
    void createFranchise_whenValid_persistsAndReturnsEntity() {
        FranchiseRepositoryPort repo = mock(FranchiseRepositoryPort.class);
        when(repo.existsByName(anyString())).thenReturn(Mono.just(false));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0, Franchise.class)));

        FranchiseCommandService service = new FranchiseCommandService(repo);

        StepVerifier.create(service.createFranchise("  Franquicia A  "))
                .assertNext(franchise -> {
                    assertNotNull(franchise.id());
                    assertEquals("Franquicia A", franchise.name());
                })
                .verifyComplete();
    }

    @Test
    void renameFranchise_whenNotFound_returnsNotFound() {
        FranchiseRepositoryPort repo = mock(FranchiseRepositoryPort.class);
        when(repo.findById(any())).thenReturn(Mono.empty());

        FranchiseCommandService service = new FranchiseCommandService(repo);

        StepVerifier.create(service.renameFranchise(UUID.randomUUID(), "Nuevo"))
                .expectError(NotFoundException.class)
                .verify();
    }
}
