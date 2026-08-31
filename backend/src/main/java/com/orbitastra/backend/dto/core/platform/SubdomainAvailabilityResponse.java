package com.orbitastra.backend.dto.core.platform;

/**
 * Whether a subdomain can be claimed. The answer to G3.
 *
 * <p>A signup or rename screen asks this while somebody is still typing, so that #1 and #10 do
 * not fail at the very end of a filled-in form.
 *
 * <p><b>Every answer is a 200, including "no".</b> "That name is taken" is a successful answer to
 * the question that was asked — the caller asked whether they could have it, and they got a real
 * reply. Only a missing {@code value} parameter is an error, and the framework handles that one.
 *
 * <p>{@code reason} carries <b>the same code the write would have thrown</b> —
 * {@code SUBDOMAIN_REQUIRED}, {@code SUBDOMAIN_INVALID}, {@code SUBDOMAIN_RESERVED} or
 * {@code SUBDOMAIN_TAKEN} — because the service gets it by running the real validator and
 * reading the code off the exception. That is on purpose: if this endpoint kept its own list of
 * rules, the two would drift and the screen would eventually promise a name the write then
 * refuses.
 *
 * <p>{@code normalized} is what the name would actually be stored as, so a screen can show it
 * back: type "St Marys" and this says "st-marys". It is filled in even when the answer is no, so
 * the caller can see what was checked.
 *
 * <p>This tells the caller whether a subdomain exists, and that is fine — a subdomain is a public
 * hostname. <b>Nothing else about the school may ever be added to this record.</b> The whole
 * point is that an unauthenticated typing-ahead check cannot be turned into a way to read
 * somebody's tenant.
 */
public record SubdomainAvailabilityResponse(
        String requested,
        String normalized,
        boolean available,
        String reason,
        String message) {

    /** The name is free. */
    public static SubdomainAvailabilityResponse available(String requested, String normalized) {
        return new SubdomainAvailabilityResponse(requested, normalized, true, null,
                "'" + normalized + "' is available.");
    }

    /** The name cannot be claimed, for the reason the write would have given. */
    public static SubdomainAvailabilityResponse unavailable(
            String requested, String normalized, String reason, String message) {
        return new SubdomainAvailabilityResponse(requested, normalized, false, reason, message);
    }
}
