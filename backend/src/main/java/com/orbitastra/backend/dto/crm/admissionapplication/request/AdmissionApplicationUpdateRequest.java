package com.orbitastra.backend.dto.crm.admissionapplication.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationCreateRequest.Guardian;
import com.orbitastra.backend.models.common.enums.Gender;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * What #18 carries — a correction to a form the family has not sent yet.
 *
 * <p><b>Families fill a form over several sittings.</b> Before this, #17 created one and nothing
 * could change it: a typo in a child's name meant starting again, because #19 freezes the snapshot
 * and #18 is the only thing that may touch it beforehand.
 *
 * <p><b>Every field is optional and a body carrying none is {@code 400 NOTHING_TO_UPDATE}</b>, the
 * same shape #27 and #29b have.
 *
 * <p><b>The CYCLE is not a field, and that is deliberate.</b> A form belongs to the round it was
 * created against — the round decides the academic year, the seat table and the window #19 checks
 * — so moving it to another round is not a correction, it is a different application. #17 is what
 * starts one.
 *
 * <p><b>Neither is the inquiry.</b> #17 takes one and moves that lead to
 * {@code APPLICATION_STARTED}; letting #18 re-point it would leave the old lead claiming a form it
 * no longer has, and this endpoint has no business writing to a second collection.
 *
 * <p><b>And neither is {@code status}.</b> {@code DRAFT → SUBMITTED} is #19, which freezes the
 * snapshot as it goes — an edit that could set the status would be a way to submit without
 * freezing anything.
 */
public record AdmissionApplicationUpdateRequest(

        /** Re-checked exactly as #17 checks it: a class of the CYCLE'S year, with seats. */
        @Size(max = 60) String appliedClassDocsId,

        @Size(max = 160) String applicantName,

        @Past LocalDate dateOfBirth,

        Gender gender,

        /**
         * The guardians, <b>replaced whole</b>.
         *
         * <p><b>The guardian shape is #17's</b>, imported rather than declared again: two records
         * with identical fields are two things that drift the first time a field is added to one.
         *
         * <p>A list is one value. Merging would leave no way to remove a guardian added by
         * mistake, and there is no id on a guardian to merge <i>by</i> — they are embedded, not
         * documents.
         *
         * <p><b>{@code @Size(min = 1)}, NOT {@code @NotEmpty}</b> — and the difference is the whole
         * of what a PATCH means. {@code @NotEmpty} rejects <i>null</i>, so on an endpoint where
         * every field is optional it would make guardians <b>required on every call</b>:
         * correcting a name would fail unless the caller re-sent the guardians too.
         * {@code @Size} ignores an absent field and still refuses an empty list, which is the rule
         * that was wanted — a form with no guardian on it is not one a school can act on, the same
         * as #17.
         *
         * <p>Found by the suite on its first run, when correcting a name answered
         * {@code guardians: must not be empty}.
         */
        @Size(min = 1, max = 10) List<@Valid Guardian> guardians,

        /**
         * The answers, <b>replaced whole</b>. {@code {}} therefore clears them.
         *
         * <p>Nothing validates the keys — there is no form-definition model, so a school names its
         * own questions — and the only rule is how many, which the service caps.
         */
        Map<String, Object> formAnswers,

        Long version) {
}
