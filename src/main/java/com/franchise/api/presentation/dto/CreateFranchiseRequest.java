package com.franchise.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFranchiseRequest(
        @NotBlank String name
) {
}

