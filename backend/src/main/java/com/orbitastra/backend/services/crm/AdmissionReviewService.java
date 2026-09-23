package com.orbitastra.backend.services.crm;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewCreateRequest;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionReview;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissionreview.AdmissionReviewRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admission reviews — how a school assesses an application. Endpoint #26 of the plan in
 * {@code controllers/crm/README.md}; only that one is built.
 *
 * <p><b>This is the first thing in the module that reads {@code staff}.</b> Everything before it
 * worked on rounds, forms and leads; a review is the first document that names a person inside the
 * school.
 *
 * <p><b>Assigning the work and doing it are two endpoints on purpose.</b> #26 says who is looking;
 * #27 records what they found. If one call did both, there would be no state in which a review is
 * outstanding — and "what is on my desk" is the whole of #28.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdmissionReviewService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /** Absent means the first round, which is what most applications get. */
    private static final int FIRST_ROUND = 1;

    /**
     * The states an application can be reviewed in.
     *
     * <p><b>It has to have been sent, and it has to not be finished.</b> A {@code DRAFT} is a form
     * the family is still filling in — reviewing it would be assessing something nobody has
     * declared yet. A {@code REJECTED}, {@code WITHDRAWN} or {@code ENROLLED} one is decided, and a
     * review assigned after the decision is work nobody will read.
     *
     * <p><b>{@code WAITLISTED} is in the set, and that is deliberate.</b> A school holding an
     * applicant for a seat often looks at them again when one comes free, and the graph allows
     * {@code WAITLISTED → APPROVED} for exactly that.
     *
     * <p><b>{@code OFFERED} and {@code OFFER_ACCEPTED} are not.</b> The school has already made its
     * offer; assessing the applicant afterwards is not a thing that can change anything.
     */
    private static final Set<AdmissionApplicationStatus> REVIEWABLE = EnumSet.of(
            AdmissionApplicationStatus.SUBMITTED,
            AdmissionApplicationStatus.UNDER_REVIEW,
            AdmissionApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED,
            AdmissionApplicationStatus.WAITLISTED);

    private final AdmissionReviewRepository admissionReviews;
    private final AdmissionApplicationRepository applications;
    private final StaffRepository staff;
    private final CurrentSchoolResolver currentSchool;

    /**
     * Endpoint #26 — puts an application on somebody's desk.
     *
     * <p>Creates in {@code PENDING}. Recording the result is #27, which is not built, so nothing
     * can move a review past {@code PENDING} yet.
     *
     * <p><b>It moves the application to {@code UNDER_REVIEW}, and only from {@code SUBMITTED}.</b>
     * That is the one status move this endpoint owns. An application already
     * {@code UNDER_REVIEW} is left where it is — the second reviewer of a round does not move
     * anything — and {@code ADDITIONAL_INFORMATION_REQUIRED → UNDER_REVIEW} belongs to #20, which
     * is what decides that the information arrived.
     */
    public AdmissionReviewResponse assignReviewer(String admissionApplicationId,
            AdmissionReviewCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String applicationId = admissionApplicationId == null ? "" : admissionApplicationId.trim();
        log.info("[assignReviewer] Step 1: Assigning a reviewer on application {} for school {}",
                applicationId, school.getId());

        //! step 2 - the form, scoped by school in the QUERY. An id from another school is a real
        //! id, and a review written against it would be this school's row pointing at another
        //! school's child.
        // TODO: read admission application
        AdmissionApplication application = applications
                .findByIdAndSchoolId(applicationId, school.getId())
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND",
                        "No admission application with id '" + applicationId
                                + "' in this school."));

        //! step 3 - it has to be a form somebody can usefully look at.
        if (!REVIEWABLE.contains(application.getStatus())) {
            throw ApiException.conflict("APPLICATION_NOT_REVIEWABLE",
                    "'" + application.getApplicantName() + "' is " + application.getStatus()
                            + ", so a review cannot be assigned. "
                            + (application.getStatus() == AdmissionApplicationStatus.DRAFT
                                    ? "The family has not submitted it yet — #19 is what sends it."
                                    : "It has already been decided, and a review assigned now is "
                                            + "work nobody would read."));
        }

        //! step 4 - the reviewer has to be this school's staff. Read rather than checked for
        //! existence, because the name is wanted on the answer and this is the read that has it.
        String reviewerId = request.reviewerDocsId().trim();

        // TODO: read staff
        Staff reviewer = staff.findByIdAndSchoolId(reviewerId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                        "No staff member with id '" + reviewerId + "' in this school, so they "
                                + "cannot be given a review."));

        //! step 5 - rounds run 1, 2, 3 with no holes. Round 3 needs a round 2 to already exist on
        //! this application, by ANYBODY — the rounds are the school's stages, not one person's, so
        //! a second assessor joining round 1 does not open round 2.
        //!
        //! ROUND 1 IS ALWAYS ALLOWED, and is what an absent reviewRound means.
        //!
        //! Nothing used to check this. A school numbering its rounds 1 and 3 was called odd rather
        //! than wrong, and that was the wrong call: a gap is somebody typing the wrong number, and
        //! the round it leaves behind can never be filled in afterwards without this refusal being
        //! in the way. Changed 2026-09-23.
        int round = request.reviewRound() == null ? FIRST_ROUND : request.reviewRound();

        // TODO: check admission review exists
        if (round > FIRST_ROUND && !admissionReviews
                .existsBySchoolIdAndAdmissionApplicationDocsIdAndReviewRound(
                        school.getId(), application.getId(), round - 1)) {
            throw ApiException.conflict("REVIEW_ROUND_OUT_OF_ORDER",
                    "'" + application.getApplicantName() + "' has no round " + (round - 1)
                            + ", so round " + round + " cannot be opened. Rounds run 1, 2, 3 with "
                            + "no gaps — open round " + (round - 1) + " first, or leave the round "
                            + "off entirely to use round 1.");
        }

        //! step 6 - one reviewer, one round, once. Asked before the insert so the refusal names
        //! the person and the round instead of being a duplicate-key error from the index.

        // TODO: check admission review exists
        if (admissionReviews
                .existsBySchoolIdAndAdmissionApplicationDocsIdAndReviewRoundAndReviewerDocsId(
                        school.getId(), application.getId(), round, reviewerId)) {
            throw ApiException.conflict("REVIEWER_ALREADY_ASSIGNED",
                    reviewer.getFullName() + " already has round " + round + " of '"
                            + application.getApplicantName() + "'. A round can hold more than one "
                            + "reviewer, but not the same one twice — assign somebody else, or "
                            + "use a different round.");
        }

        //! step 7 - build the review
        AdmissionReview review = AdmissionReview.builder()
                .admissionApplicationDocsId(application.getId())
                .reviewRound(round)
                .reviewerDocsId(reviewerId)
                .reviewerRole(request.reviewerRole().trim())
                .status(AdmissionReviewStatus.PENDING)
                .dueAt(request.dueAt())
                .notes(TextHelper.blankToNull(request.notes()))
                .build();

        //! SET EXPLICITLY. Every SchoolBase insert names its school: nothing fills this in, and a
        //! review without one belongs to every school at once.
        review.setSchoolId(school.getId());

        //! step 8 - save
        // TODO: insert admission review
        AdmissionReview saved = admissionReviews.save(review);
        log.info("[assignReviewer] Step 2: Saved review {} for reviewer {} on round {}",
                saved.getId(), reviewerId, round);

        //! step 9 - the form is being looked at now.
        //!
        //! ONLY FROM SUBMITTED. A form already UNDER_REVIEW stays there — the second reviewer of a
        //! round moves nothing — and ADDITIONAL_INFORMATION_REQUIRED and WAITLISTED are moved by
        //! #20, which is what decides they are ready to be looked at again.
        //!
        //! DONE AFTER THE REVIEW IS SAVED, not before: if the insert fails, an application that
        //! says UNDER_REVIEW with nobody reviewing it is a worse lie than one that is late.
        if (application.getStatus() == AdmissionApplicationStatus.SUBMITTED) {
            application.setStatus(AdmissionApplicationStatus.UNDER_REVIEW);

            // TODO: update admission application
            applications.save(application);
            log.info("[assignReviewer] Step 3: Moved application {} to UNDER_REVIEW",
                    application.getId());
        }

        return AdmissionReviewResponse.fromReview(saved, application.getApplicationNo(),
                reviewer.getFullName(),
                reviewer.getFullName() + " has round " + round + " of '"
                        + application.getApplicantName() + "' as " + saved.getReviewerRole()
                        + ". Recording what they found is #27, which is not built, so this review "
                        + "cannot move past PENDING yet. " + NO_AUTHORIZATION_YET);
    }
}
