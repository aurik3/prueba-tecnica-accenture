package com.franchise.api.presentation.util;

import com.franchise.api.domain.exception.InvalidInputException;

import java.util.UUID;

public final class UuidParser {

    private UuidParser() {
    }

    public static UUID parse(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException(fieldName + " is required");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException(fieldName + " must be a valid UUID");
        }
    }
}

