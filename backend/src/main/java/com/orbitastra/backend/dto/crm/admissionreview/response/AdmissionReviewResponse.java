package com.orbitastra.backend.dto.crm.admissionreview.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.crm.AdmissionReview;
import com.orbitastra.backend.models.crm.enums.AdmissionRecommendation;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

/**
 * One admission review, as the API gives it back. Endpoint #26, and #27 when it is built.
 *
 * <p><b>The reviewer comes back NAMED.</b> #26 has to read the staff record anyway — to refuse an
 * id that is not this school's — so resolving the name costs nothing, and a response carrying only
 * {@code reviewerDocsId} would make the caller do a second call to draw one row.
 */
public record AdmissionReviewResponse(

        String admissionReviewId,

        /** The form this is a review of. */
        String admissionApplicationDocsId,

        /** Its number, so a caller can say which form without a second call. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String applicationNo,

        Integer reviewRound,
        String reviewerDocsId,

        /** Resolved from the staff record #26 already read. */
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

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, BigDecimal> criterionScores,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String notes,

        Instant createdAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    public static AdmissionReviewResponse fromReview(AdmissionReview review, String applicationNo,
            String reviewerName, String nextStep) {

        return new AdmissionReviewResponse(
                review.getId(),
                review.getAdmissionApplicationDocsId(),
                applicationNo,
                review.getReviewRound(),
                review.getReviewerDocsId(),
                reviewerName,
                review.getReviewerRole(),
                review.getStatus(),
                review.getDueAt(),
                review.getCompletedAt(),
                review.getScore(),
                review.getRecommendation(),
                // Left out when empty rather than sent as {} — a fresh assignment has no scores,
                // and an empty map on every one of them is noise.
                review.getCriterionScores() == null || review.getCriterionScores().isEmpty()
                        ? null : review.getCriterionScores(),
                review.getNotes(),
                review.getCreatedAt(),
                nextStep);
    }
}
