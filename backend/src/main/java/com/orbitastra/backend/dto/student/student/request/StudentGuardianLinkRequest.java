package com.orbitastra.backend.dto.student.student.request;

import com.orbitastra.backend.models.common.enums.GuardianRelation;
import com.orbitastra.backend.models.common.enums.SchoolLocale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What #11 needs to put a guardian on a child.
 *
 * <h2>It does both jobs, and that is wider than the plan asked for</h2>
 *
 * <p>The plan made {@code guardianDocsId} <b>required</b> — link only, with #7 for creating. Widened
 * 2026-10-07 because the desk does not work that way: somebody adding a father to a child types his
 * name and his number, and whether the school already holds him is not something they know before
 * they start.
 *
 * <ul>
 *   <li><b>Send {@code guardianDocsId}</b> and that person is linked. The fields below are ignored
 *       — the stored row wins, and correcting it is #8.</li>
 *   <li><b>Leave it out</b> and {@code fullName} is required, a new guardian is written, and the
 *       phone and the email must be <b>free</b>.</li>
 * </ul>
 *
 * <p><b>A taken number is the same refusal #1 gives</b>, naming the holder and quoting their id —
 * which is exactly what the caller sends back to link them instead. That round trip is the whole
 * flow: type a number, be told whose it is, decide, link.
 *
 * <h2>The relation and the flags are always this child's</h2>
 *
 * <p>Even when an existing guardian is linked. The same man is "father, primary contact, may
 * collect, portal" to one child and only an emergency number for their cousin — so these are never
 * read off the guardian, and never written back to them.
 *
 * <p><b>{@code primaryContact: true} clears it on the child's other guardians</b>, in the same
 * write. Two primaries is not a state worth being able to reach, and the response says who was
 * demoted.
 */
public record StudentGuardianLinkRequest(

        /**
         * An existing guardian in this school. <b>Optional.</b>
         *
         * <p>Sent: that person is linked and everything below except the relation and the flags is
         * ignored. Left out: a new guardian is written from the fields below.
         */
        @Size(max = 60) String guardianDocsId,

        /** <b>Required when {@code guardianDocsId} is not sent</b>, ignored when it is. */
        @Size(max = 160) String fullName,

        /** <b>The identity of a guardian in this school.</b> Must be free when creating one. */
        @Size(max = 40) String phoneNumber,

        /** Not unique and not checked — a shared family landline. */
        @Size(max = 40) String alternatePhoneNumber,

        /** Unique per school the same way the phone is. */
        @Email @Size(max = 160) String emailAddress,

        @Size(max = 400) String address,

        @Size(max = 120) String occupation,

        SchoolLocale preferredLanguage,

        /**
         * What this person is <b>to this child</b>. Always required — it is the one thing a link
         * cannot be without, and it is never read off the guardian.
         */
        @NotNull GuardianRelation relation,

        /** <b>Setting this clears it on the child's other guardians.</b> Default false. */
        Boolean primaryContact,

        Boolean emergencyContact,

        /** May collect the child. Default false. */
        Boolean pickupAuthorized,

        /** May be given a login to the guardian portal, when that portal exists. Default false. */
        Boolean portalAccess,

        /**
         * Optimistic check <b>on the student</b>, not on the guardian — the link lives in the
         * child's document, so that is what this write touches.
         *
         * <p>Required. Leaving it out is {@code 400 VALIDATION_FAILED}.
         */
        @NotNull Long version) {

    /** Trims the email before {@code @Email} judges it, the same as every other request here. */
    public StudentGuardianLinkRequest {
        emailAddress = emailAddress == null ? null : emailAddress.trim();
    }
}
