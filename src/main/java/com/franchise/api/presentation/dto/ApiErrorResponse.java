package com.franchise.api.presentation.dto;

import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
}

