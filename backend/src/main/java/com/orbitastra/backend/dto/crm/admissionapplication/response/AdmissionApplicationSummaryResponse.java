package com.orbitastra.backend.dto.crm.admissionapplication.response;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

/**
 * One row of #24's list.
 *
 * <p><b>Thinner than what #17 gives back, and deliberately so.</b> Three fields are left off:
 *
 * <ul>
 *   <li>{@code guardians} — a list per row, and a hundred rows is a lot of contact details nobody
 *       reads on a worklist.</li>
 *   <li>{@code formAnswers} — an unbounded map with no form definition behind it.</li>
 *   <li>{@code evidenceDocumentDocsIds} — a list of ids that resolve to nothing here.</li>
 * </ul>
 *
 * <p>All three are on #25, which opens one application.
 *
 * <p><b>No {@code appliedClassName}.</b> #24 reads one collection — the plan says so — and
 * resolving names would mean a second. A row carries the class id, and a caller that wants names
 * has the class list.
 *
 * <p><b>No {@code nextStep}:</b> a read changed nothing.
 */
public record AdmissionApplicationSummaryResponse(

        String admissionApplicationId,
        String applicationNo,
        String admissionCycleDocsId,
        String appliedClassDocsId,
        String applicantName,
        LocalDate dateOfBirth,
        Gender gender,
        AdmissionApplicationStatus status,

        /** Absent on a walk-in — the family that never enquired. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String inquiryDocsId,

        /** Absent until #22 puts the form on somebody's worklist. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedAdmissionOfficerDocsId,

        /**
         * That person's name, resolved in <b>one query for the whole page</b> — never one per row.
         *
         * <p><b>Written when #22 arrived</b>, and not before: until something could fill the field
         * this branch had nothing to resolve and no way to be tested. A worklist filtered by
         * officer that shows raw ids is not a worklist anybody can work from.
         *
         * <p>Absent when nobody is assigned, and also when the assigned officer has left the
         * school — which is the honest answer rather than an invented one.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedAdmissionOfficerName,

        /** Absent while the form is still a DRAFT. Submitting is #19, which is not built. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant submittedAt,

        /** When the form was started. What the default order sorts on. */
        Instant createdAt) {

    public static AdmissionApplicationSummaryResponse fromApplication(AdmissionApplication one,
            String assignedAdmissionOfficerName) {
        return new AdmissionApplicationSummaryResponse(
                one.getId(),
                one.getApplicationNo(),
                one.getAdmissionCycleDocsId(),
                one.getAppliedClassDocsId(),
                one.getApplicantName(),
                one.getDateOfBirth(),
                one.getGender(),
                one.getStatus(),
                one.getInquiryDocsId(),
                one.getAssignedAdmissionOfficerDocsId(),
                assignedAdmissionOfficerName,
                one.getSubmittedAt(),
                one.getCreatedAt());
    }
}
