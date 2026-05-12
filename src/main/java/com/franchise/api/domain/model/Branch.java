package com.franchise.api.domain.model;

import java.util.UUID;

public record Branch(UUID id, UUID franchiseId, String name) {

    public static Branch create(UUID id, UUID franchiseId, String name) {
        return new Branch(id, franchiseId, DomainValidators.normalizeName(name, "name"));
    }

    public Branch rename(String newName) {
        return new Branch(id, franchiseId, DomainValidators.normalizeName(newName, "name"));
    }
}

