package com.orbitastra.backend.dto.crm.admissionapplication.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.AdmissionReview;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionRecommendation;
import com.orbitastra.backend.models.crm.enums.AdmissionResponse;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

/**
 * One admission application in full. Endpoint #25.
 *
 * <p><b>What this has that a #24 list row does not:</b> the guardians, the form answers, the
 * evidence ids, everything about withdrawal and enrollment — and the <b>reviews and offers</b>,
 * which live in their own collections and are read here rather than stored on the application.
 *
 * <p><b>Why a row cannot carry those.</b> A form takes up to 200 answers and any number of
 * guardians; twenty rows of a list would carry four thousand answers to draw a table that shows
 * none of them. The split is the same one #5 and #6 make about a cycle's seat table.
 *
 * <p><b>The reviews and offers are empty today, and that is a real answer rather than a gap.</b>
 * #26 and #27 create reviews; #29 to #31 create offers; none are built. The queries run, they are
 * scoped to this school, and they return nothing because there is nothing — the same answer they
 * will give for an application nobody has reviewed long after those endpoints exist.
 *
 * <p><b>The class, the cycle and the REVIEWERS are named; the officer is not.</b> A raw
 * {@code classDocsId} is not something a person can read, so this resolves them.
 *
 * <p>The reviewers were ids until 2026-09-22, on the grounds that #26 was not built and a
 * name-resolving branch could never run or be tested. #26 arrived, so the branch was written — in
 * <b>one query for every reviewer on the form</b>, not one per review.
 *
 * <p><b>The assigned officer is still an id</b>, and for the same reason the reviewers used to be:
 * #22 is what fills that field and it is not built, so nothing can put a name behind it yet.
 */
public record AdmissionApplicationDetailResponse(

        /** The id reviews, offers and the resulting student all point at. */
        String admissionApplicationId,

        /** Generated, never supplied. Nobody picks their own application number. */
        String applicationNo,

        String admissionCycleDocsId,

        /**
         * What the school calls the round this form went into.
         *
         * <p><b>Absent when the cycle itself is gone.</b> That is honest rather than tidy: an
         * application pointing at a round the database no longer holds is a real problem, and
         * inventing a name — or refusing the whole read — would hide it. Same call #6 makes about
         * a seat row naming a deleted class.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String admissionCycleName,

        /** The year the round admits for. Absent for the same reason as the name. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String academicYear,

        /** The lead this came from, when it came from one. A walk-in has none. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String inquiryDocsId,

        String appliedClassDocsId,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String appliedClassName,

        String applicantName,
        LocalDate dateOfBirth,
        Gender gender,
        AdmissionApplicationStatus status,

        /**
         * The guardians as the family declared them.
         *
         * <p><b>A snapshot, not a link.</b> Copied from the inquiry when there was one and
         * editable until #19 submits the form, after which it is frozen — a school that could
         * rewrite these afterwards could not answer "what did they actually tell us".
         *
         * <p>The same shape #17 gives back, reused rather than redeclared: two records with
         * identical fields are two records that drift apart.
         */
        List<AdmissionApplicationResponse.Guardian> guardians,

        /**
         * The extra answers this school asked for. Left out entirely when there are none.
         *
         * <p>Nothing validates these — there is no form definition to check them against — so what
         * comes back is exactly what was sent.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Map<String, Object> formAnswers,

        /**
         * The documents attached as evidence, as {@code DocumentRecord} ids.
         *
         * <p><b>Ids, not files.</b> This module stores the ids; {@code documents} owns the files
         * and is what turns one into something downloadable. Empty until #23 attaches any.
         */
        List<String> evidenceDocumentDocsIds,

        /**
         * Whose worklist this sits in. <b>Always absent today</b> — #22 assigns an officer and is
         * not built, so nothing can fill it.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String assignedAdmissionOfficerDocsId,

        /** When the family submitted it. Absent while it is still a {@code DRAFT}. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant submittedAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Instant withdrawnAt,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String withdrawalReason,

        /** The student this became. Absent until #33 enrolls them. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String resultingStudentDocsId,

        /** Every review of this application, oldest round first. Empty until #26 exists. */
        List<Review> reviews,

        /** How many there are, so a caller does not have to count an array it may not read. */
        int reviewCount,

        /** Every offer made on it, first revision first — superseded ones included. */
        List<Offer> offers,

        int offerCount,

        Instant createdAt,
        Instant updatedAt,

        /** What can be done to this application next, in plain words. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String nextStep) {

    /**
     * Everything read and resolved, turned into the answer.
     *
     * <p>Takes the names already looked up rather than looking them up itself: a DTO that queried
     * would hide a database call inside what looks like a mapping, and the one query for every
     * class is the whole reason the service resolves them together.
     *
     * @param cycleName        null when the round is gone — see the field's note
     * @param academicYear     null for the same reason
     * @param appliedClassName null when the class is gone, or when the round is
     * @param classNames       every class id this application or its offers name, to its name
     * @param reviewerNames    every reviewer id on this form, to their name
     */
    public static AdmissionApplicationDetailResponse fromApplication(
            AdmissionApplication application,
            String cycleName,
            String academicYear,
            String appliedClassName,
            List<AdmissionReview> reviews,
            List<AdmissionOffer> offers,
            Map<String, String> classNames,
            Map<String, String> reviewerNames,
            String nextStep) {

        List<Review> reviewRows = reviews == null ? List.of()
                : reviews.stream()
                        .map(one -> Review.fromReview(one,
                                one.getReviewerDocsId() == null ? null
                                        : reviewerNames.get(one.getReviewerDocsId())))
                        .toList();

        List<Offer> offerRows = offers == null ? List.of()
                : offers.stream()
                        .map(offer -> Offer.fromOffer(offer,
                                offer.getOfferedClassDocsId() == null ? null
                                        : classNames.get(offer.getOfferedClassDocsId())))
                        .toList();

        return new AdmissionApplicationDetailResponse(
                application.getId(),
                application.getApplicationNo(),
                application.getAdmissionCycleDocsId(),
                cycleName,
                academicYear,
                application.getInquiryDocsId(),
                application.getAppliedClassDocsId(),
                appliedClassName,
                application.getApplicantName(),
                application.getDateOfBirth(),
                application.getGender(),
                application.getStatus(),
                AdmissionApplicationResponse.Guardian.fromGuardians(application.getGuardians()),
                // Left out when empty rather than sent as {} — an absent map and an empty one say
                // the same thing, and one of them is noise.
                application.getFormAnswers() == null || application.getFormAnswers().isEmpty()
                        ? null : application.getFormAnswers(),
                application.getEvidenceDocumentDocsIds() == null ? List.of()
                        : application.getEvidenceDocumentDocsIds(),
                application.getAssignedAdmissionOfficerDocsId(),
                application.getSubmittedAt(),
                application.getWithdrawnAt(),
                application.getWithdrawalReason(),
                application.getResultingStudentDocsId(),
                reviewRows,
                reviewRows.size(),
                offerRows,
                offerRows.size(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                nextStep);
    }

    /**
     * One review of this application.
     *
     * <p><b>A reviewer who has left reads back with no name</b>, exactly as a seat row naming a
     * deleted class does on #6. The review stays and is marked by its absence rather than dropped:
     * a form assessed by somebody the school no longer employs is still a form that was assessed.
     */
    public record Review(
            String admissionReviewId,
            Integer reviewRound,
            String reviewerDocsId,

            /** Resolved with every other reviewer on this form, in one query. */
            @JsonInclude(JsonInclude.Include.NON_NULL) String reviewerName,

            String reviewerRole,
            AdmissionReviewStatus status,

            @JsonInclude(JsonInclude.Include.NON_NULL) Instant dueAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) Instant completedAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal score,
            @JsonInclude(JsonInclude.Include.NON_NULL) AdmissionRecommendation recommendation,
            @JsonInclude(JsonInclude.Include.NON_NULL) String notes,

            /** What each part scored — interview, entrance test. Left out when empty. */
            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, BigDecimal> criterionScores) {

        static Review fromReview(AdmissionReview review, String reviewerName) {
            return new Review(
                    review.getId(),
                    review.getReviewRound(),
                    review.getReviewerDocsId(),
                    reviewerName,
                    review.getReviewerRole(),
                    review.getStatus(),
                    review.getDueAt(),
                    review.getCompletedAt(),
                    review.getScore(),
                    review.getRecommendation(),
                    review.getNotes(),
                    review.getCriterionScores() == null || review.getCriterionScores().isEmpty()
                            ? null : review.getCriterionScores());
        }
    }

    /**
     * One offer made on this application.
     *
     * <p><b>The offered class can differ from the applied one</b>, which is why it is carried
     * separately: a school assesses a child and offers a different grade.
     */
    public record Offer(
            String admissionOfferId,
            String offerNo,
            Integer revisionNo,
            String offeredClassDocsId,

            @JsonInclude(JsonInclude.Include.NON_NULL) String offeredClassName,

            AdmissionOfferStatus status,

            @JsonInclude(JsonInclude.Include.NON_NULL) Instant offeredAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) Instant expiresAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) Instant respondedAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) AdmissionResponse response,
            @JsonInclude(JsonInclude.Include.NON_NULL) String withdrawalReason) {

        static Offer fromOffer(AdmissionOffer offer, String offeredClassName) {
            return new Offer(
                    offer.getId(),
                    offer.getOfferNo(),
                    offer.getRevisionNo(),
                    offer.getOfferedClassDocsId(),
                    offeredClassName,
                    offer.getStatus(),
                    offer.getOfferedAt(),
                    offer.getExpiresAt(),
                    offer.getRespondedAt(),
                    offer.getResponse(),
                    offer.getWithdrawalReason());
        }
    }
}
