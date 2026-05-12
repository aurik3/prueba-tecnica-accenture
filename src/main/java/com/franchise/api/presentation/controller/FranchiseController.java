package com.franchise.api.presentation.controller;

import com.franchise.api.application.model.BranchMaxStockProduct;
import com.franchise.api.application.service.BranchCommandService;
import com.franchise.api.application.service.FranchiseCommandService;
import com.franchise.api.application.service.FranchiseQueryService;
import com.franchise.api.domain.model.Branch;
import com.franchise.api.domain.model.Franchise;
import com.franchise.api.presentation.dto.BranchMaxStockProductResponse;
import com.franchise.api.presentation.dto.BranchResponse;
import com.franchise.api.presentation.dto.CreateBranchRequest;
import com.franchise.api.presentation.dto.CreateFranchiseRequest;
import com.franchise.api.presentation.dto.FranchiseResponse;
import com.franchise.api.presentation.dto.RenameRequest;
import com.franchise.api.presentation.util.UuidParser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/franchises")
public class FranchiseController {

    private final FranchiseCommandService franchiseCommandService;
    private final BranchCommandService branchCommandService;
    private final FranchiseQueryService franchiseQueryService;

    public FranchiseController(FranchiseCommandService franchiseCommandService,
                               BranchCommandService branchCommandService,
                               FranchiseQueryService franchiseQueryService) {
        this.franchiseCommandService = franchiseCommandService;
        this.branchCommandService = branchCommandService;
        this.franchiseQueryService = franchiseQueryService;
    }

    @PostMapping
    public Mono<ResponseEntity<FranchiseResponse>> createFranchise(@Valid @RequestBody CreateFranchiseRequest request) {
        return franchiseCommandService.createFranchise(request.name())
                .map(franchise -> ResponseEntity
                        .created(URI.create("/api/franchises/" + franchise.id()))
                        .body(toResponse(franchise)));
    }

    @GetMapping
    public Flux<FranchiseResponse> getFranchises() {
        return franchiseQueryService.getAllFranchises().map(this::toResponse);
    }

    @PatchMapping("/{franchiseId}")
    public Mono<FranchiseResponse> renameFranchise(@PathVariable String franchiseId, @Valid @RequestBody RenameRequest request) {
        UUID id = UuidParser.parse(franchiseId, "franchiseId");
        return franchiseCommandService.renameFranchise(id, request.name()).map(this::toResponse);
    }

    @GetMapping("/{franchiseId}")
    public Mono<FranchiseResponse> getFranchise(@PathVariable String franchiseId) {
        UUID id = UuidParser.parse(franchiseId, "franchiseId");
        return franchiseQueryService.getFranchiseById(id).map(this::toResponse);
    }

    @PostMapping("/{franchiseId}/branches")
    public Mono<ResponseEntity<BranchResponse>> addBranch(@PathVariable String franchiseId, @Valid @RequestBody CreateBranchRequest request) {
        UUID id = UuidParser.parse(franchiseId, "franchiseId");
        return branchCommandService.addBranchToFranchise(id, request.name())
                .map(branch -> ResponseEntity
                        .created(URI.create("/api/branches/" + branch.id()))
                        .body(toResponse(branch)));
    }

    @GetMapping("/{franchiseId}/branches")
    public Flux<BranchResponse> getBranches(@PathVariable String franchiseId) {
        UUID id = UuidParser.parse(franchiseId, "franchiseId");
        return franchiseQueryService.getBranchesByFranchise(id).map(this::toResponse);
    }

    @GetMapping("/{franchiseId}/branches/max-stock-products")
    public Flux<BranchMaxStockProductResponse> getMaxStockProducts(@PathVariable String franchiseId) {
        UUID id = UuidParser.parse(franchiseId, "franchiseId");
        return franchiseQueryService.getMaxStockProductsByBranch(id).map(this::toResponse);
    }

    private FranchiseResponse toResponse(Franchise franchise) {
        return new FranchiseResponse(franchise.id().toString(), franchise.name());
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(branch.id().toString(), branch.franchiseId().toString(), branch.name());
    }

    private BranchMaxStockProductResponse toResponse(BranchMaxStockProduct item) {
        return new BranchMaxStockProductResponse(
                item.branchId().toString(),
                item.branchName(),
                new BranchMaxStockProductResponse.ProductSummary(item.productId().toString(), item.productName(), item.stock())
        );
    }
}
