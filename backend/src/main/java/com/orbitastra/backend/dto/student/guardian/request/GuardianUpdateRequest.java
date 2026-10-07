package com.orbitastra.backend.dto.student.guardian.request;

import com.orbitastra.backend.models.common.enums.SchoolLocale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What #8 may correct on a guardian.
 *
 * <h2>This changes the person for EVERY child linked to them</h2>
 *
 * <p>That is the point of the shared row, and it is the one thing worth knowing before using this
 * endpoint. A guardian is one real person per school — their phone number says so — so correcting
 * a mother's number here corrects it on all four of her children at once. <b>There is no way to
 * change it for one of them</b>, and there should not be: the alternative is four rows for one
 * woman and no way to tell which is current.
 *
 * <p><b>The response says how many children were affected</b>, so a caller who did not expect that
 * finds out immediately rather than from a parent.
 *
 * <h2>What is not here</h2>
 *
 * <p><b>The relation and the four flags.</b> {@code FATHER}, "primary contact", "may collect",
 * "portal" are facts about a person <i>and a child together</i> — the same man is all four to one
 * child and only an emergency number for their cousin. They live on {@code GuardianLink}, embedded
 * in the student, and changing them is #12. Putting them here would mean changing somebody's
 * relation to <i>all</i> their children at once, which is not a thing that happens.
 *
 * <h2>{@code ""} clears, absent leaves alone</h2>
 *
 * <p>The project-wide rule, and the reason this is not a {@code PUT}: a form that sent every field
 * would wipe whatever it did not know about.
 *
 * <p><b>{@code fullName} is the exception</b> — blank is refused rather than obeyed, because the
 * model requires one and a guardian with no name is a row nobody can find.
 *
 * <p><b>{@code preferredLanguage} can be corrected but not removed</b>, and that is a limitation
 * rather than a decision: it is an enum, so {@code ""} is not a value it takes, and {@code null}
 * already means "leave it alone". The same limitation {@code student} #2 and {@code people} #2
 * record, for the same reason.
 */
public record GuardianUpdateRequest(

        /**
         * <b>Cannot be removed, only corrected.</b> A blank is
         * {@code 400 GUARDIAN_NAME_REQUIRED}.
         */
        @Size(max = 160) String fullName,

        /**
         * <b>The identity of a guardian in this school.</b> Changing it is allowed and checked:
         * another person's number is {@code 409 GUARDIAN_PHONE_TAKEN}.
         *
         * <p><b>Re-sending the number they already have is not a conflict</b> — the check skips
         * the guardian being corrected, or editing somebody's name would refuse on their own
         * phone.
         *
         * <p>{@code ""} removes it, which is allowed: a guardian with no number is a guardian
         * nothing identifies, and that is a state #7 can create too.
         */
        @Size(max = 40) String phoneNumber,

        /** Not unique and not checked — a shared family landline. {@code ""} removes it. */
        @Size(max = 40) String alternatePhoneNumber,

        /** Unique per school the same way the phone is, and skipped for themselves the same way. */
        @Email @Size(max = 160) String emailAddress,

        @Size(max = 400) String address,

        @Size(max = 120) String occupation,

        /** Correctable but not removable — see the note above on enums. */
        SchoolLocale preferredLanguage,

        /**
         * Optimistic check. <b>Required</b> — leaving it out is {@code 400 VALIDATION_FAILED}.
         *
         * <p>It matters more here than on most writes: this row is shared, so two offices
         * correcting one mother's number are genuinely likely to collide.
         */
        @NotNull Long version) {

    /**
     * Trims the email before anything judges it.
     *
     * <p>{@code @Email} runs on the constructed record and a compact constructor runs first, so
     * without this a paste out of a spreadsheet is refused as malformed. The trim also turns a
     * field of spaces into {@code ""}, which is the "clear it" value rather than a bad address.
     */
    public GuardianUpdateRequest {
        emailAddress = emailAddress == null ? null : emailAddress.trim();
    }

    /** Whether the request asks for nothing at all. The version alone is not a change. */
    public boolean isEmpty() {
        return fullName == null && phoneNumber == null && alternatePhoneNumber == null
                && emailAddress == null && address == null && occupation == null
                && preferredLanguage == null;
    }
}
