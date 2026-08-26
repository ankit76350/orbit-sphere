package com.orbitastra.backend.common.error.exception;

/**
 * The request was well formed, but the current state of things does not allow it.
 *
 * <p>Answers HTTP 409, and the distinction from a 400 matters more in this system than usual.
 * A subdomain already taken, a school already activated, two academic years overlapping — none
 * of those are bad requests. The caller sent something perfectly valid and the answer is still
 * no, so a 400 would send them off checking their JSON for a mistake that is not there.
 *
 * <p>Every lifecycle transition in {@code controllers/core} leans on this: anything not on the
 * documented SchoolStatus path is a conflict, not a validation failure.
 */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
