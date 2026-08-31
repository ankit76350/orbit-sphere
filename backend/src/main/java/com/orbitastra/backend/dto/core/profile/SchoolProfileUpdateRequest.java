package com.orbitastra.backend.dto.core;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * A partial edit of the school's own details. Endpoint #6.
 *
 * <p><b>How PATCH behaves here, because absent and null look the same to Jackson:</b>
 *
 * <pre>
 * field omitted, or null   -> leave it exactly as it is
 * field is ""              -> clear it (set to null in the database)
 * field has a value        -> replace it
 * </pre>
 *
 * <p>That convention exists because a record cannot tell "the caller did not mention this" from
 * "the caller sent null". Without it, an optional field could never be cleared: every PATCH that
 * omitted a phone number would look identical to one asking to remove it, and the safe reading —
 * leave it alone — would win forever.
 *
 * <p>{@code schoolName} cannot be cleared. It is {@code @NotBlank} on the model, and a school
 * with no name is not a state worth supporting, so {@code ""} there is a 400 rather than a
 * deletion.
 *
 * <p>Nothing here can change the tenant's identity or lifecycle. Subdomain, status and the
 * encryption key are all on the platform surface, and the fields simply do not exist on this
 * record — which is what stops a school admin reaching them.
 */
public record SchoolProfileUpdateRequest(

        /** Example: "Orbit Astra International School". Cannot be cleared. */
        @Size(max = 200) String schoolName,

        /** Example: "+919876543210". Send "" to remove it. */
        @Size(max = 30) String phoneNumber,

        /** Example: "office@orbit-school.edu". Send "" to remove it. */
        @Size(max = 254) String emailAddress) {

    /** True when the caller asked for nothing at all — answered with a 400, not a silent 200. */
    public boolean isEmpty() {
        return schoolName == null && phoneNumber == null && emailAddress == null;
    }

    /**
     * {@code @Email} is not on the field itself because it rejects {@code ""}, and {@code ""} is
     * how this endpoint clears a value. Checked in the service instead, only when there is
     * something to check.
     */
    @Email
    public String emailForValidation() {
        return emailAddress == null || emailAddress.isEmpty() ? null : emailAddress;
    }
}
