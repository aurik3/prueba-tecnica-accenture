package com.franchise.api.domain.model;

import java.util.UUID;

public record Franchise(UUID id, String name) {

    public static Franchise create(UUID id, String name) {
        return new Franchise(id, DomainValidators.normalizeName(name, "name"));
    }

    public Franchise rename(String newName) {
        return new Franchise(id, DomainValidators.normalizeName(newName, "name"));
    }
}

