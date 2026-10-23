package com.orbitastra.backend.dto.crm.admissionreview.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionReview;
import com.orbitastra.backend.models.crm.enums.AdmissionRecommendation;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

/**
 * One row of #28's queue.
 *
 * <p><b>Thinner than what #26 and #27 give back:</b> no {@code criterionScores} and no
 * {@code notes}. A review can carry fifty criteria and two thousand characters of notes, and a
 * twenty-row page would haul all of it to draw a list that shows neither. Both are on the review
 * itself.
 *
 * <p><b>But the applicant IS named.</b> A queue of raw application ids is not a queue anybody can
 * work from, and it is one query for the whole page rather than one per row.
 */
public record AdmissionReviewSummaryResponse(

        String admissionReviewId,
        String admissionApplicationDocsId,

        /** Resolved for the whole page at once. Absent if the form is gone. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String applicationNo,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String applicantName,

        Integer reviewRound,
        String reviewerDocsId,

        /** Resolved for the whole page at once. Absent if they are not staff any more. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String reviewerName,

        String reviewerRole,
        AdmissionReviewStatus status,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant dueAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant completedAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal score,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        AdmissionRecommendation recommendation,

        Instant createdAt,

        /** What #27 must send back. A queue row is where somebody picks the review to record on. */
        Long version) {

    public static AdmissionReviewSummaryResponse fromReview(AdmissionReview review,
            String applicationNo, String applicantName, String reviewerName) {

        return new AdmissionReviewSummaryResponse(
                review.getId(),
                review.getAdmissionApplicationDocsId(),
                applicationNo,
                applicantName,
                review.getReviewRound(),
                review.getReviewerDocsId(),
                reviewerName,
                review.getReviewerRole(),
                review.getStatus(),
                review.getDueAt(),
                review.getCompletedAt(),
                review.getScore(),
                review.getRecommendation(),
                review.getCreatedAt(),
                review.getVersion());
    }
}
