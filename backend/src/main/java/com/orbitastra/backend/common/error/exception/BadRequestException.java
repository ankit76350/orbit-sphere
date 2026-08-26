package com.orbitastra.backend.common.error.exception;

/**
 * The request itself is wrong — a value is missing, or is not a usable value at all.
 *
 * <p>Answers HTTP 400, and the line against {@link ConflictException} is worth holding to.
 * A 400 says <i>this is not a well-formed request</i>: no subdomain was sent, a date is not a
 * date. A 409 says <i>the request is fine and the answer is still no</i>: the subdomain is
 * spelled correctly and somebody else already has it.
 *
 * <p>Getting that backwards costs a caller real time. Told 400 for a taken subdomain, they go
 * hunting their JSON for a mistake that is not there. Told 409 for a missing field, they think
 * the server is refusing something rather than that they forgot to send it.
 *
 * <p>Most 400s should never reach this class. Required, length, a simple regex, `@Email` all
 * belong as Jakarta annotations on the request record: they run before the controller method is
 * entered and produce per-field errors a form can put beside the right input, which is strictly
 * better than one message. Use this where an annotation cannot express the rule but the value
 * is still malformed rather than merely unavailable.
 */
public class BadRequestException extends RuntimeException {

    private final String code;

    public BadRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
