package com.orbitastra.backend.dto.core.profile;

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
 * <p>{@code schoolName} and {@code accountHolderName} cannot be cleared. Both are
 * {@code @NotBlank} on the model, and a school with no name — or no named account holder — is
 * not a state worth supporting, so {@code ""} there is a 400 rather than a deletion.
 *
 * <p>Nothing here can change the tenant's identity or lifecycle. Subdomain, status and the
 * encryption key are all on the platform surface, and the fields simply do not exist on this
 * record — which is what stops a school admin reaching them.
 */
public record SchoolProfileUpdateRequest(

        /** Example: "Orbit Astra International School". Cannot be cleared. */
        @Size(max = 200) String schoolName,

        /**
         * Who the school names as the holder of this account. Example: "Rohan Shinde".
         * Cannot be cleared.
         *
         * <p>Moved here on 2026-08-31 from its own platform endpoint (#11 in the plan), which
         * was dropped. It is a plain label — nothing links it to a UserAccount, nothing checks it
         * and nothing is granted by it — so a separate platform-only endpoint for one
         * unreferenced string was ceremony. It edits like {@code schoolName}, so it lives beside
         * it.
         *
         * <p>If it ever becomes contractual — who signed, who gets billed — the fix is to link it
         * to a real account rather than to move it back out of here.
         */
        @Size(max = 200) String accountHolderName,

        /** Example: "+919876543210". Send "" to remove it. */
        @Size(max = 30) String phoneNumber,

        /** Example: "office@orbit-school.edu". Send "" to remove it. */
        @Size(max = 254) String emailAddress) {

    /** True when the caller asked for nothing at all — answered with a 400, not a silent 200. */
    public boolean isEmpty() {
        return schoolName == null && accountHolderName == null && phoneNumber == null
                && emailAddress == null;
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
