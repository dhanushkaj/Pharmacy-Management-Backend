package com.rdp.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Global exception handler returning JSON payloads so frontend can display messages.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    public static record ErrorResponse(String message) {}

    /**
     * Handle bean validation exceptions raised by Hibernate/JPA during persist/merge.
     * Return first violation formatted as "field: message" when possible.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.debug("ConstraintViolationException: {} violations", ex.getConstraintViolations().size(), ex);

        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> formatConstraintViolation(v))
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    private String formatConstraintViolation(ConstraintViolation<?> v) {
        String path = Optional.ofNullable(v.getPropertyPath()).map(Object::toString).orElse("");
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        if (field.isBlank()) field = "value";
        String msg = Optional.ofNullable(v.getMessage()).orElse("Validation failed");
        return field + ": " + msg;
    }

    /**
     * Handle @Valid failures on controller method arguments (request body validation).
     * Return first field error as "field: message".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.debug("MethodArgumentNotValidException: binding errors = {}", ex.getBindingResult().getErrorCount(), ex);

        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> {
                    String field = Optional.ofNullable(fe.getField()).orElse("value");
                    String msg = Optional.ofNullable(fe.getDefaultMessage()).orElse("Validation failed");
                    return field + ": " + msg;
                }).orElse("Validation failed");

        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    /**
     * Handle IllegalArgumentException and IllegalStateException - return 400 with the message.
     * Useful for friendly error messages like "Category is still referenced by products".
     */
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ErrorResponse> handleBadRequestRuntime(RuntimeException ex) {
        log.debug("Bad request: {}", ex.getMessage(), ex);
        String msg = Optional.ofNullable(ex.getMessage()).orElse("Bad request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(msg));
    }

    /**
     * Handle DB constraint violations (unique, not null, FK) surfaced through Spring's DataIntegrityViolationException.
     * Return 400 with a friendly single-line message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException", ex);
        String root = Optional.ofNullable(ex.getRootCause()).map(Throwable::getMessage).orElse(ex.getMessage());
        String normalized = normalizeDbMessage(root);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(normalized));
    }

    private String normalizeDbMessage(String dbMessage) {
        if (dbMessage == null) return "Database constraint violation";
        String lower = dbMessage.toLowerCase();

        // not-null violation (Postgres)
        if (lower.contains("null value in column") && lower.contains("violates not-null constraint")) {
            int colStart = lower.indexOf("column \"");
            if (colStart >= 0) {
                int start = colStart + "column \"".length();
                int end = dbMessage.indexOf("\"", start);
                if (end > start) {
                    String col = dbMessage.substring(start, end);
                    return col + " cannot be null";
                }
            }
            return "Missing required value";
        }

        // duplicate key / unique constraint
        if (lower.contains("duplicate key") || lower.contains("unique constraint") || lower.contains("duplicate value")) {
            return "Duplicate value violates unique constraint";
        }

        String trimmed = dbMessage.length() > 250 ? dbMessage.substring(0, 250) + "…" : dbMessage;
        return trimmed;
    }

    /**
     * Generic fallback: return 500 with minimal message (do not leak stacktrace).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        String msg = Optional.ofNullable(ex.getMessage()).orElse("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(msg));
    }
}
