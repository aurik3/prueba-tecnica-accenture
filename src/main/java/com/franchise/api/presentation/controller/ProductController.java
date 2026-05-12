package com.franchise.api.presentation.controller;

import com.franchise.api.application.service.FranchiseQueryService;
import com.franchise.api.application.service.ProductCommandService;
import com.franchise.api.domain.model.Product;
import com.franchise.api.presentation.dto.CreateProductRequest;
import com.franchise.api.presentation.dto.ProductResponse;
import com.franchise.api.presentation.dto.RenameRequest;
import com.franchise.api.presentation.dto.UpdateStockRequest;
import com.franchise.api.presentation.util.UuidParser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api")
public class ProductController {

    private final ProductCommandService productCommandService;
    private final FranchiseQueryService franchiseQueryService;

    public ProductController(ProductCommandService productCommandService, FranchiseQueryService franchiseQueryService) {
        this.productCommandService = productCommandService;
        this.franchiseQueryService = franchiseQueryService;
    }

    @PostMapping("/branches/{branchId}/products")
    public Mono<ResponseEntity<ProductResponse>> addProduct(@PathVariable String branchId, @Valid @RequestBody CreateProductRequest request) {
        UUID bId = UuidParser.parse(branchId, "branchId");
        return productCommandService.addProductToBranch(bId, request.name(), request.stock())
                .map(product -> ResponseEntity
                        .created(URI.create("/api/branches/" + bId + "/products/" + product.id()))
                        .body(toResponse(product)));
    }

    @GetMapping("/branches/{branchId}/products")
    public Flux<ProductResponse> getProductsByBranch(@PathVariable String branchId) {
        UUID bId = UuidParser.parse(branchId, "branchId");
        return franchiseQueryService.getProductsByBranch(bId).map(this::toResponse);
    }

    @GetMapping("/branches/{branchId}/products/{productId}")
    public Mono<ProductResponse> getProduct(@PathVariable String branchId, @PathVariable String productId) {
        UUID bId = UuidParser.parse(branchId, "branchId");
        UUID pId = UuidParser.parse(productId, "productId");
        return franchiseQueryService.getProductByIdInBranch(bId, pId).map(this::toResponse);
    }

    @DeleteMapping("/branches/{branchId}/products/{productId}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String branchId, @PathVariable String productId) {
        UUID bId = UuidParser.parse(branchId, "branchId");
        UUID pId = UuidParser.parse(productId, "productId");
        return productCommandService.deleteProductFromBranch(bId, pId).thenReturn(ResponseEntity.noContent().build());
    }

    @PatchMapping("/branches/{branchId}/products/{productId}/stock")
    public Mono<ProductResponse> updateStock(@PathVariable String branchId, @PathVariable String productId, @Valid @RequestBody UpdateStockRequest request) {
        UUID bId = UuidParser.parse(branchId, "branchId");
        UUID pId = UuidParser.parse(productId, "productId");
        return productCommandService.updateProductStock(bId, pId, request.stock()).map(this::toResponse);
    }

    @PatchMapping("/branches/{branchId}/products/{productId}")
    public Mono<ProductResponse> renameProduct(@PathVariable String branchId, @PathVariable String productId, @Valid @RequestBody RenameRequest request) {
        UUID bId = UuidParser.parse(branchId, "branchId");
        UUID pId = UuidParser.parse(productId, "productId");
        return productCommandService.renameProduct(bId, pId, request.name()).map(this::toResponse);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.id().toString(), product.branchId().toString(), product.name(), product.stock());
    }
}
