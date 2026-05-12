package com.franchise.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(
        @NotBlank String name
) {
}

