package com.franchise.api.domain.model;

import com.franchise.api.domain.exception.InvalidInputException;

public final class DomainValidators {

    private DomainValidators() {
    }

    public static String normalizeName(String name, String fieldName) {
        if (name == null) {
            throw new InvalidInputException(fieldName + " is required");
        }
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new InvalidInputException(fieldName + " is required");
        }
        if (normalized.length() > 120) {
            throw new InvalidInputException(fieldName + " must be at most 120 characters");
        }
        return normalized;
    }

    public static long validateStock(Long stock) {
        if (stock == null) {
            throw new InvalidInputException("stock is required");
        }
        if (stock < 0) {
            throw new InvalidInputException("stock must be greater than or equal to 0");
        }
        return stock;
    }
}

