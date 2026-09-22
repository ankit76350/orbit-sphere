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

        /** Absent until #22 assigns one, which is not built. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedAdmissionOfficerDocsId,

        /** Absent while the form is still a DRAFT. Submitting is #19, which is not built. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant submittedAt,

        /** When the form was started. What the default order sorts on. */
        Instant createdAt) {

    public static AdmissionApplicationSummaryResponse fromApplication(AdmissionApplication one) {
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
                one.getSubmittedAt(),
                one.getCreatedAt());
    }
}
