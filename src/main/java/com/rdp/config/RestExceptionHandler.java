package com.rdp.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Global exception handler returning JSON payloads so frontend can display messages.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    public static record ErrorResponse(String message) {}

    /**
     * Handle bean validation exceptions raised by Hibernate/JPA during persist/merge.
     * Example in logs: jakarta.validation.ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        // Get the first violation message only (without field name)
        String message = ex.getConstraintViolations().stream()
                .map(v -> Optional.ofNullable(v.getMessage()).orElse("Validation failed"))
                .findFirst()
                .orElse("Validation failed");

        ErrorResponse body = new ErrorResponse(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handle @Valid argument failures in controllers (request body validation).
     * This returns the first field error message only (without field name).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("Validation failed"))
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    /**
     * Handle DB constraint violations (unique, not null, FK) surfaced through Spring's DataIntegrityViolationException.
     * Return 400 with the root message to display to the user.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = Optional.ofNullable(ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage())
                .orElse("Database constraint violation");
        // You may want to map certain DB messages to friendlier messages here.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(msg));
    }

    /**
     * Generic fallback: return 500 with minimal message (do not leak stacktrace).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        // log server side for debugging (use proper logger in production)
        ex.printStackTrace();

        String msg = Optional.ofNullable(ex.getMessage()).orElse("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(msg));
    }
}