package com.franchise.api.presentation.controller;

import com.franchise.api.presentation.dto.RootResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
public class RootController {

    @GetMapping("/")
    public Mono<RootResponse> root(ServerHttpRequest request) {
        String docs = request.getURI().resolve("/swagger-ui.html").toString();
        return Mono.just(new RootResponse(
                "ok",
                "3.0",
                "Prueba Tecnica  Backend Java Springboot Fredy Alejandro Gonzalez Caro",
                docs
        ));
    }

    @GetMapping("/swagger-ui/index.html")
    public Mono<ResponseEntity<Void>> redirectSwaggerIndex() {
        return Mono.just(ResponseEntity.status(302).location(URI.create("/swagger-ui.html")).build());
    }
}
