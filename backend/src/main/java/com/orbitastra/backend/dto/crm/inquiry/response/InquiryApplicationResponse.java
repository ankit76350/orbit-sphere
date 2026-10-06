package com.orbitastra.backend.dto.crm.inquiry.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

/**
 * One row of #16 — <b>an application this lead turned into</b>.
 *
 * <p><b>Why it is not {@code AdmissionApplicationSummaryResponse}.</b> That row is a worklist row:
 * it answers "who is this and where in the pipeline are they", because #24 is read by somebody
 * working a queue of a hundred forms. #16 is read by somebody standing on <i>one</i> lead asking
 * what came of it, and the answer to that is the <b>ending</b> — decided when and with what note,
 * withdrawn and why, and the student id if it got that far. The worklist row carries none of those
 * four.
 *
 * <p><b>It drops {@code inquiryDocsId}</b>, which the worklist row carries. Every row here is an
 * application of the lead in the path; repeating the id in all of them says nothing.
 *
 * <p><b>It drops {@code dateOfBirth}, {@code gender} and the guardians too.</b> They are the
 * child's details, and the child is the lead the caller is already looking at. A form that
 * disagrees with the lead is worth seeing — but that is #25, which opens the form itself.
 *
 * <p><b>No class name, no cycle name.</b> #16 reads one collection, the same call #24 made: names
 * would mean two more. The ids are here and the caller has the lists.
 *
 * <p><b>No {@code nextStep}.</b> A read changed nothing.
 */
public record InquiryApplicationResponse(

        String admissionApplicationId,
        String applicationNo,

        /** Which admission round the family applied in. The lead can appear in several. */
        String admissionCycleDocsId,
        String appliedClassDocsId,

        /**
         * The name on the <b>form</b>, which can differ from the one on the lead: the front desk
         * writes down what it hears on the phone, and the family types what is on the certificate.
         */
        String applicantName,
        AdmissionApplicationStatus status,

        /** When the form was started. What this list is ordered by, newest first. */
        Instant createdAt,

        /** Absent while the form is still a {@code DRAFT}. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant submittedAt,

        /** Absent until the school has decided. #20 stamps it. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant decidedAt,

        /** Why the school decided what it did. Required on #20 since 2026-10-01. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String decisionNote,

        /** Absent unless the family pulled out. #21 stamps it. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant withdrawnAt,

        /** The reason #21 asked the caller for. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String withdrawalReason,

        /**
         * The student the applicant became. <b>The one field that makes this endpoint's sentence
         * true</b> — "what the lead became" has a literal answer only when it is set. #33 writes
         * it, and #33 is not built, so it is absent on every row today.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String resultingStudentDocsId) {

    public static InquiryApplicationResponse fromApplication(AdmissionApplication one) {
        return new InquiryApplicationResponse(
                one.getId(),
                one.getApplicationNo(),
                one.getAdmissionCycleDocsId(),
                one.getAppliedClassDocsId(),
                one.getApplicantName(),
                one.getStatus(),
                one.getCreatedAt(),
                one.getSubmittedAt(),
                one.getDecidedAt(),
                one.getDecisionNote(),
                one.getWithdrawnAt(),
                one.getWithdrawalReason(),
                one.getResultingStudentDocsId());
    }
}
