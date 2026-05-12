package com.franchise.api.infrastructure.mongo.mapper;

import java.util.UUID;

public final class IdMapper {

    private IdMapper() {
    }

    public static String toString(UUID id) {
        return id == null ? null : id.toString();
    }

    public static UUID toUuid(String id) {
        return id == null ? null : UUID.fromString(id);
    }
}

