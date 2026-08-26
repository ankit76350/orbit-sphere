package com.orbitastra.backend.common.error.exception;

/**
 * The thing named in the path does not exist.
 *
 * <p>Answers HTTP 404. For tenant-scoped lookups this must also be the answer when a record
 * exists but belongs to another school. **Never a 403 in that case** — telling a caller "that
 * exists but is not yours" confirms the record is real, which is enough to enumerate another
 * school's data one id at a time.
 */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
