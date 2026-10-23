package com.orbitastra.backend.dto.student.guardian.request;

import com.orbitastra.backend.models.common.enums.SchoolLocale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What #7 needs to add a guardian who is not being created with a child.
 *
 * <h2>Why this exists when #1 already makes guardians</h2>
 *
 * <p>#1 makes them <i>as a side effect of admitting a child</i>. This is for the guardian who turns
 * up on their own: a grandmother added before the child she will collect, an emergency number the
 * office wants on file, or a family the school is entering ahead of an admission.
 *
 * <p><b>No relation and no flags.</b> {@code FATHER}, "primary contact", "may collect" are facts
 * about a person <i>and a child together</i> — the same man is "father, primary, portal" to one
 * child and only an emergency number for their cousin. They live on the link, which is #11.
 * A guardian created here belongs to nobody yet, and that is a normal state rather than a
 * half-finished one.
 *
 * <h2>It REFUSES a taken number. #1 matches one. That is deliberate</h2>
 *
 * <p>The database says a phone number identifies one person per school —
 * {@code school_guardian_phone_uniq} — and both endpoints obey it. What they do when they meet one
 * is opposite, because the caller is saying something different:
 *
 * <ul>
 *   <li><b>#1 is describing a family.</b> "This child's father is on 98765 43210" — if the school
 *       already knows that man, the right answer is to link him, and refusing would make a
 *       sibling's admission fail for no reason the desk can act on.</li>
 *   <li><b>#7 is asserting a new person.</b> "Add this guardian" — if the number is already
 *       somebody's, either the caller has the wrong number or they meant to edit that person, and
 *       quietly handing back an existing row would look like a successful create and leave them
 *       believing a guardian exists that does not.</li>
 * </ul>
 *
 * <p>So this answers {@code 409 GUARDIAN_PHONE_TAKEN} or {@code 409 GUARDIAN_EMAIL_TAKEN}, and the
 * message names the person who holds it so the caller can go and look.
 */
public record GuardianCreateRequest(

        @NotBlank @Size(max = 160) String fullName,

        /**
         * <b>The identity of a guardian in this school</b>, as far as the database is concerned.
         * Stored with spaces, brackets, hyphens and dots taken off, so "+91 98765 43210" and
         * "+919876543210" are the same person — and the same normalising #1 does, because the two
         * write into one unique index.
         *
         * <p>Optional: a guardian with no number at all is allowed, and nothing then identifies
         * them.
         */
        @Size(max = 40) String phoneNumber,

        /**
         * A second number for the same person — often "my husband's phone".
         *
         * <p><b>Not unique and not checked</b>, because a family shares one landline and refusing
         * it would make a mother and a father impossible to enter.
         */
        @Size(max = 40) String alternatePhoneNumber,

        /** Stored lowercase, and unique per school the same way the phone is. */
        @Email @Size(max = 160) String emailAddress,

        @Size(max = 400) String address,

        @Size(max = 120) String occupation,

        /** The language this person is written to in. */
        SchoolLocale preferredLanguage) {

    /**
     * Trims the email before anything judges it.
     *
     * <p>{@code @Email} runs on the constructed record and a compact constructor runs first, so
     * without this a paste out of a spreadsheet is refused as malformed.
     */
    public GuardianCreateRequest {
        emailAddress = emailAddress == null ? null : emailAddress.trim();
    }
}
