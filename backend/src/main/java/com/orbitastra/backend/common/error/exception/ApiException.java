package com.orbitastra.backend.common.error.exception;

import org.springframework.http.HttpStatus;

/**
 * Common exception for API errors.
 *
 * <p>Use the factory methods to create errors with the correct HTTP status.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    // Private to force the use of factory methods.
    private ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * Creates a 400 Bad Request error.
     */
    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    /**
     * Creates a 409 Conflict error.
     */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    /**
     * Creates a 404 Not Found error.
     */
    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}