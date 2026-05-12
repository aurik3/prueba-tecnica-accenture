package com.franchise.api.presentation;

import com.franchise.api.presentation.dto.BranchMaxStockProductResponse;
import com.franchise.api.presentation.dto.BranchResponse;
import com.franchise.api.presentation.dto.CreateBranchRequest;
import com.franchise.api.presentation.dto.CreateFranchiseRequest;
import com.franchise.api.presentation.dto.CreateProductRequest;
import com.franchise.api.presentation.dto.FranchiseResponse;
import com.franchise.api.presentation.dto.ProductResponse;
import com.franchise.api.presentation.dto.UpdateStockRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FranchiseApiIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.data.mongodb.auto-index-creation", () -> "true");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void endToEnd_happyPath() {
        FranchiseResponse franchise = webTestClient.post()
                .uri("/api/franchises")
                .bodyValue(new CreateFranchiseRequest("Franquicia A"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(franchise);
        assertNotNull(franchise.id());

        BranchResponse branch = webTestClient.post()
                .uri("/api/franchises/{id}/branches", franchise.id())
                .bodyValue(new CreateBranchRequest("Sucursal 1"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(branch);
        assertNotNull(branch.id());

        ProductResponse p1 = webTestClient.post()
                .uri("/api/branches/{id}/products", branch.id())
                .bodyValue(new CreateProductRequest("Producto A", 10L))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        ProductResponse p2 = webTestClient.post()
                .uri("/api/branches/{id}/products", branch.id())
                .bodyValue(new CreateProductRequest("Producto B", 50L))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(p1);
        assertNotNull(p2);

        ProductResponse updated = webTestClient.patch()
                .uri("/api/branches/{branchId}/products/{productId}/stock", branch.id(), p1.id())
                .bodyValue(new UpdateStockRequest(80L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(updated);
        assertEquals(80L, updated.stock());

        List<BranchMaxStockProductResponse> maxStock = webTestClient.get()
                .uri("/api/franchises/{id}/branches/max-stock-products", franchise.id())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BranchMaxStockProductResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(maxStock);
        assertEquals(1, maxStock.size());
        assertEquals(branch.id(), maxStock.get(0).branchId());
        assertEquals("Producto A", maxStock.get(0).product().name());
        assertEquals(80L, maxStock.get(0).product().stock());

        webTestClient.delete()
                .uri("/api/branches/{branchId}/products/{productId}", branch.id(), p2.id())
                .exchange()
                .expectStatus().isNoContent();
    }
}
