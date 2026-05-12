package com.franchise.api.presentation.error;

import com.franchise.api.domain.exception.ConflictException;
import com.franchise.api.domain.exception.InvalidInputException;
import com.franchise.api.domain.exception.NotFoundException;
import com.franchise.api.presentation.dto.ApiErrorResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleValidation(WebExchangeBindException ex) {
        List<Map<String, Object>> errors = ex.getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("errors", errors);
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed", details)));
    }

    @ExceptionHandler(InvalidInputException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleInvalidInput(InvalidInputException ex) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_INPUT", ex.getMessage(), Map.of())));
    }

    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleNotFound(NotFoundException ex) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage(), Map.of())));
    }

    @ExceptionHandler({ConflictException.class, DuplicateKeyException.class})
    public Mono<ResponseEntity<ApiErrorResponse>> handleConflict(Exception ex) {
        String message = ex instanceof DuplicateKeyException ? "resource already exists" : ex.getMessage();
        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("CONFLICT", message, Map.of())));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolved = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        String message = ex.getReason() == null || ex.getReason().isBlank() ? resolved.getReasonPhrase() : ex.getReason();
        String code = resolved == HttpStatus.NOT_FOUND ? "RESOURCE_NOT_FOUND" : "HTTP_ERROR";
        return Mono.just(ResponseEntity
                .status(resolved)
                .body(new ApiErrorResponse(code, message, Map.of())));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiErrorResponse>> handleUnexpected(Exception ex) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", "Unexpected error", Map.of())));
    }

    private Map<String, Object> toFieldError(FieldError error) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", error.getField());
        item.put("message", error.getDefaultMessage());
        return item;
    }
}
