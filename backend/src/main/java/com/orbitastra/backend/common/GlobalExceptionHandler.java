package com.orbitastra.backend.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.orbitastra.backend.common.exception.ConflictException;
import com.orbitastra.backend.common.exception.NotFoundException;

/**
 * Turns exceptions into the single ApiError shape.
 *
 * <p>The DuplicateKeyException handler is doing real work rather than tidying up. Several
 * uniqueness rules in this system are enforced only by a MongoDB unique index — the globally
 * unique {@code subdomain} among them — and a race between two requests gets past any
 * application-level check. When that happens the database raises a duplicate key error, and
 * without this handler the caller sees a 500 for something that is genuinely a 409.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> fields
                .computeIfAbsent(error.getField(), key -> new ArrayList<>())
                .add(error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiError.validation("One or more fields are invalid.", fields));
    }

    /**
     * The body could not be parsed at all — malformed JSON, a string where a number belongs, an
     * absent field mapped onto a primitive.
     *
     * <p>Without this handler Spring's default error page answers instead, and with devtools on
     * that body carries a **full stack trace**: package names, framework versions, the request
     * path and the failing field. That is free reconnaissance, and it reaches anyone who can
     * send a bad request. The message here says what was wrong and nothing about the inside of
     * the application.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiError.of("MALFORMED_REQUEST",
                "The request body could not be read. Check that it is valid JSON and that every "
                        + "field has the expected type."));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> onConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> onNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> onDuplicateKey(DuplicateKeyException exception) {
        // A unique index rejected the write. The application check that should have caught it
        // lost a race, which is normal under concurrency and not a server fault.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("DUPLICATE_KEY",
                        "That value is already in use. Another request may have just taken it."));
    }
}
