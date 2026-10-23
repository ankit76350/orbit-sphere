package com.orbitastra.backend.dto.people.staff.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.SchoolLocale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Edits to one person. Endpoint #2.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A request that sends
 * nothing is a {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, so a client with a bug
 * in its form finds out.
 *
 * <h2>It edits everything on the person, which absorbs #3, #4 and #5</h2>
 *
 * <p>The plan split the profile across four endpoints: this one for the scalar fields,
 * {@code PUT /addresses} (#3), {@code PUT /emergency-contact} (#4) and {@code PUT /photo} (#5).
 * <b>Asked for as one endpoint on 2026-09-15</b>, the same call that folded #11 into #10.
 *
 * <p><b>The reasoning behind the split is kept, not discarded.</b> #3 existed because "same as
 * current" is a real answer and two independent PATCHes leave a window where the pair disagree;
 * #4 because a contact with a new name beside an old number is worse than no contact, since
 * somebody will trust it in the one situation where it matters. So here:
 *
 * <p><b>An address or the emergency contact is REPLACED WHOLE, never merged.</b> Send the object
 * and it becomes the object; send the fields you want, not the fields that changed. Merging would
 * reintroduce exactly the half-updated address the split was designed to prevent.
 *
 * <h2>What this deliberately cannot change</h2>
 *
 * <p><b>Never {@code employeeNo}.</b> It is generated, and it is printed on things — an identity
 * card, a payslip, a register signed at the gate. A rename leaves a paper trail pointing at
 * nobody, and unlike a department code there is not even a second key to find them by.
 *
 * <p><b>Nothing about the job.</b> There is no status, department or joining date on this
 * document to edit — that is {@code EmploymentRecord}, and #16 writes it.
 *
 * <h2>What can be cleared, and what cannot</h2>
 *
 * <pre>
 * "currentAddress": {}      clears it       (an empty object is no address)
 * "emergencyContact": {}    clears it
 * "profileImageDocsId": ""  clears it
 * "fullName": ""            400 STAFF_NAME_REQUIRED
 * "phoneNumber": ""         400 — required since 2026-09-15
 * "emailAddress": ""        400 — required since 2026-09-15
 * any field: null           leaves it       (same as absent)
 * </pre>
 *
 * <p><b>{@code nationalityCode} and {@code preferredLanguage} can be corrected but not removed</b>,
 * and that is a limitation rather than a decision. They are enums, so {@code ""} is not a value
 * they take, and {@code null} already means "leave it alone" — with no way to tell an absent field
 * from an explicit null, there is nothing left to mean "clear". Making them clearable needs
 * {@code JsonNullable} (the {@code jackson-databind-nullable} module), which this project does not
 * depend on. Worth adding if a school ever needs to un-say somebody's nationality.
 *
 * <h2>The phone and the email stay unique, and the check excludes this person</h2>
 *
 * <p>{@code 409 STAFF_PHONE_TAKEN} and {@code 409 STAFF_EMAIL_TAKEN}. <b>Re-sending somebody their
 * own number is not a collision</b>, so the check is skipped when the normalised value already
 * belongs to the person being edited — the same shape #14 uses for a position's title.
 */
public record StaffUpdateRequest(

        /** A new name. Blank is refused, not treated as a clear. */
        @Size(max = 120) String fullName,

        /**
         * A corrected date of birth. Must be in the past.
         *
         * <p><b>It cannot be removed</b>, only corrected: the model declares it {@code @NotNull}.
         */
        @Past LocalDate dateOfBirth,

        /** A corrected gender. One of the three, and it cannot be cleared — the model requires it. */
        Gender gender,

        /** A corrected nationality. See the class note: correctable, not clearable. */
        CountryCode nationalityCode,

        /** A corrected language, from the set this product has strings for. Not clearable. */
        SchoolLocale preferredLanguage,

        /** Stripped of spacing characters. Required, unique per school, and cannot be cleared. */
        @Size(max = 32) String phoneNumber,

        /** Trimmed and lower-cased. Required, unique per school, and cannot be cleared. */
        @Email @Size(max = 160) String emailAddress,

        /** Replaced whole. {@code {}} clears it. Absent leaves it alone. */
        @Valid StaffAddressRequest currentAddress,

        /** Replaced whole, the same way. "Same as current" is a real answer, so send it twice. */
        @Valid StaffAddressRequest permanentAddress,

        /** Replaced whole. A new name beside an old number is worse than no contact at all. */
        @Valid EmergencyContactRequest emergencyContact,

        /** A {@code DocumentRecord} id, or {@code ""} to remove the photo. */
        @Size(max = 60) String profileImageDocsId) {

    /**
     * Trims the email before anything judges it — the same reason #1's request record does.
     *
     * <p>{@code @Email} runs on the constructed record and a compact constructor runs first, so
     * without this a paste from a spreadsheet is refused as malformed.
     */
    public StaffUpdateRequest {
        emailAddress = emailAddress == null ? null : emailAddress.trim();
    }

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return fullName == null && dateOfBirth == null && gender == null
                && nationalityCode == null && preferredLanguage == null
                && phoneNumber == null && emailAddress == null
                && currentAddress == null && permanentAddress == null
                && emergencyContact == null && profileImageDocsId == null;
    }
}
