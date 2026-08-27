package com.orbitastra.backend.common.error.exception;

import org.springframework.http.HttpStatus;

/**
 * One exception for every error this API raises on purpose.
 *
 * <p>Replaces BadRequestException, ConflictException and NotFoundException, which were three
 * classes with identical bodies — a code, a message, and a getter. The only thing that differed
 * was the class name, and the only thing the name was used for was picking an HTTP status in
 * GlobalExceptionHandler. So the status moved in here and the three classes became one.
 *
 * <p><b>Always throw through a factory, never the constructor.</b> The constructor is private on
 * purpose:
 *
 * <pre>
 * throw ApiException.conflict("SUBDOMAIN_TAKEN", "That subdomain is already in use.");
 * throw ApiException.badRequest("SUBDOMAIN_REQUIRED", "A subdomain is required.");
 * throw ApiException.notFound("SCHOOL_NOT_FOUND", "No school with that id.");
 * </pre>
 *
 * <p>That matters for more than tidiness. Business code — CoreValidator, SchoolService — must
 * not import {@code HttpStatus}. A validator that knows about HTTP is a validator that cannot be
 * reused off a web request, and it drags a web dependency into the one layer that should be
 * plain. The factories keep {@code HttpStatus} inside this file.
 *
 * <p>A private constructor also stops the obvious mistake: nobody can raise an ApiException with
 * status 200, or 500, or whatever an IDE autocompletes to.
 *
 * <p>{@code code} is the stable string a client branches on. {@code message} is for a person
 * reading a log and is expected to be reworded, so a client must never match on it.
 *
 * <p>Adding a status is three lines — see the factories below. Do not add one before something
 * throws it.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * 400 — the request is not well formed. A required value is missing, or a value cannot be
     * used at all.
     *
     * <p>Most 400s should never reach here. Required, length, a simple regex and {@code @Email}
     * belong as Jakarta annotations on the request record: they run before the controller is
     * entered and produce per-field errors a form can put beside the right input, which is
     * strictly better than one message. Use this where an annotation cannot express the rule.
     */
    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    /**
     * 409 — the request is fine and the answer is still no.
     *
     * <p>The subdomain is spelled correctly and somebody already has it. The time zone is a
     * reasonable guess that does not exist. The school is already activated.
     *
     * <p>Not 400. Told 400 for a taken subdomain, a caller goes hunting their JSON for a mistake
     * that is not there.
     */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    /**
     * 404 — the thing named in the path does not exist.
     *
     * <p>For a tenant-scoped lookup this is also the right answer when the record exists but
     * belongs to another school. <b>Never 403 in that case</b> — telling a caller "that exists
     * but is not yours" confirms the record is real, which is enough to enumerate another
     * school's data one id at a time.
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
