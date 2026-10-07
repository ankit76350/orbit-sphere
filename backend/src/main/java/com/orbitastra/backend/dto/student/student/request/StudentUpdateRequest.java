package com.orbitastra.backend.dto.student.student.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.common.enums.SchoolLocale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * What #2 may correct on a child.
 *
 * <p><b>The photo is not here.</b> {@code profilePhotoDocumentId} was taken off on 2026-10-07: it
 * names a {@code DocumentRecord}, and a file is uploaded rather than typed. Nothing writes that
 * field today — #1 does not accept one either — so it stays on the model waiting for the documents
 * module, which owns the upload this would otherwise have been the tail end of.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A field sent as
 * {@code ""} <i>clears</i> it where the model allows nothing there. A request that asks for
 * nothing at all is {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, because a client
 * with a broken form should not look healthy.
 *
 * <h2>What is deliberately not here</h2>
 *
 * <p><b>{@code admissionNo}.</b> It is generated, and it is printed on certificates and receipts
 * — renaming it leaves a paper trail pointing at nobody.
 *
 * <p><b>{@code status}.</b> Seven values with real preconditions on each move, which is #3 and a
 * verb rather than a field. A {@code PATCH} that set it would be seven endpoints wearing one name,
 * and it would let somebody write {@code GRADUATED} onto a child who left in March.
 *
 * <p><b>{@code guardians}.</b> They are their own documents, shared between siblings, and
 * attaching or detaching one is #11 to #13. A list replaced whole here would quietly unlink a
 * father from a child whose form simply did not mention him.
 *
 * <p><b>{@code currentAcademicRecordDocsId}</b> is owned by the record endpoints — #14 and #17
 * write it — and <b>{@code admissionApplicationDocsId}</b> is written once by
 * {@code crm} #33 and is the link back to how this child arrived.
 *
 * <p><b>{@code admissionDate} is not in the plan's list either</b>, and that is worth knowing
 * rather than assuming: a date typed wrongly when a school entered its existing roll cannot be
 * corrected through this endpoint today.
 *
 * <h2>Two fields can be corrected but not removed, and that is a limitation</h2>
 *
 * <p>{@code nationalityCode} and {@code preferredLanguage} are enums, so {@code ""} is not a value
 * they take — and {@code null} already means "leave it alone", with no way to tell an absent field
 * from one deliberately emptied. Expressing both would need {@code JsonNullable}, which this
 * project does not use. The same limitation {@code people} #2 records, for the same reason.
 *
 * <p>{@code dateOfBirth} and {@code gender} cannot be removed either, but that is a <i>rule</i>
 * rather than a limitation: the model requires both.
 */
public record StudentUpdateRequest(

        /**
         * <b>Cannot be removed, only corrected.</b> The model requires one, and it is the thing a
         * child is found by — a blank is {@code 400 STUDENT_NAME_REQUIRED} rather than a silent
         * clear.
         */
        @Size(max = 160) String fullName,

        /**
         * Correctable; a transposed year is the most ordinary mistake on an admission form.
         *
         * <p><b>It cannot be removed</b>: the model declares it required.
         */
        @Past LocalDate dateOfBirth,

        /** Correctable, not removable, for the same reason. */
        Gender gender,

        /** Correctable but not removable — see the note above on enums. */
        CountryCode nationalityCode,

        /** The same. */
        SchoolLocale preferredLanguage,

        /** The child's <b>own</b> number, for an older student. {@code ""} removes it. */
        @Size(max = 40) String phoneNumber,

        /** The child's own address. {@code ""} removes it. */
        @Email @Size(max = 160) String emailAddress,

        /**
         * Optimistic check. <b>Required</b> — leaving it out is {@code 400 VALIDATION_FAILED}.
         *
         * <p>A caller who cannot say what they read cannot be told their read was stale, and two
         * clerks correcting one child's record is exactly what this exists for.
         */
        @NotNull Long version) {

    /**
     * Trims the email before anything judges it.
     *
     * <p>{@code @Email} runs on the constructed record and a compact constructor runs first, so
     * without this a paste out of a spreadsheet is refused as malformed. The trim also turns a
     * field of spaces into {@code ""}, which is the "clear it" value rather than a bad address.
     */
    public StudentUpdateRequest {
        emailAddress = emailAddress == null ? null : emailAddress.trim();
    }

    /** Whether the request asks for nothing at all. The version alone is not a change. */
    public boolean isEmpty() {
        return fullName == null && dateOfBirth == null && gender == null
                && nationalityCode == null && preferredLanguage == null
                && phoneNumber == null && emailAddress == null;
    }
}
