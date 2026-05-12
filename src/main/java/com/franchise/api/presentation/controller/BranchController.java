package com.franchise.api.presentation.controller;

import com.franchise.api.application.service.BranchCommandService;
import com.franchise.api.application.service.FranchiseQueryService;
import com.franchise.api.domain.model.Branch;
import com.franchise.api.presentation.dto.BranchResponse;
import com.franchise.api.presentation.dto.RenameRequest;
import com.franchise.api.presentation.util.UuidParser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchCommandService branchCommandService;
    private final FranchiseQueryService franchiseQueryService;

    public BranchController(BranchCommandService branchCommandService, FranchiseQueryService franchiseQueryService) {
        this.branchCommandService = branchCommandService;
        this.franchiseQueryService = franchiseQueryService;
    }

    @PatchMapping("/{branchId}")
    public Mono<BranchResponse> renameBranch(@PathVariable String branchId, @Valid @RequestBody RenameRequest request) {
        UUID id = UuidParser.parse(branchId, "branchId");
        return branchCommandService.renameBranch(id, request.name()).map(this::toResponse);
    }

    @GetMapping
    public Flux<BranchResponse> getBranches() {
        return franchiseQueryService.getAllBranches().map(this::toResponse);
    }

    @GetMapping("/{branchId}")
    public Mono<BranchResponse> getBranch(@PathVariable String branchId) {
        UUID id = UuidParser.parse(branchId, "branchId");
        return franchiseQueryService.getBranchById(id).map(this::toResponse);
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(branch.id().toString(), branch.franchiseId().toString(), branch.name());
    }
}
