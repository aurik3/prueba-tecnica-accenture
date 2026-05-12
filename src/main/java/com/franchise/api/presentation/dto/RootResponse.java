package com.franchise.api.presentation.dto;

public record RootResponse(
        String status,
        String version,
        String data,
        String docs
) {
}

