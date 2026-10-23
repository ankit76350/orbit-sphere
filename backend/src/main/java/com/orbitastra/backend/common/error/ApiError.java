package com.orbitastra.backend.common.error;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The one error body every endpoint returns.
 *
 * <p>{@code code} is a stable machine-readable string a client can branch on;
 * {@code message} is for a person reading a log. Clients must never match on the message —
 * it is expected to be reworded.
 *
 * <p>{@code fieldErrors} is filled only for validation failures, keyed by field path, so a form
 * can put each message next to the input that caused it rather than showing one banner.
 */
public record ApiError(
        String code,
        String message,
        Map<String, List<String>> fieldErrors,
        Instant timestamp) {

    // Here, we have two methods for creating errors to throw in the API response.  
    public static ApiError createError(String code, String message) {
        return new ApiError(code, message, null, Instant.now());
    }

    public static ApiError createValidationError(
            String message,
            Map<String, List<String>> fieldErrors) {
        return new ApiError("VALIDATION_FAILED", message, fieldErrors, Instant.now());
    }
}
