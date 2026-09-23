package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewCreateRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewSearchRequest;
import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewUpdateRequest;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewResponse;
import com.orbitastra.backend.dto.crm.admissionreview.response.AdmissionReviewSummaryResponse;
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
 * Admission reviews — how a school assesses an application. Endpoints #26, #27 and #28 of the plan in
 * {@code controllers/crm/README.md}; all three are built.
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

    /**
     * What #27 may move a review to, from where. Mirrors {@code CYCLE_MOVES} on #3 and
     * {@code DECISION_MOVES} on #20.
     *
     * <p><b>Both ends are terminal.</b> A {@code COMPLETED} review is a record of what somebody
     * found, and a {@code CANCELLED} one of work the school called off — neither is a draft to be
     * revised. A score typed wrong is corrected by cancelling the review and assigning another,
     * which leaves both in the history rather than quietly overwriting one.
     *
     * <p><b>{@code PENDING → COMPLETED} skips {@code IN_PROGRESS}</b>, and that is deliberate.
     * Most reviews are done in one sitting, and forcing a "I have started" call first would be
     * ceremony nobody would keep up.
     */
    private static final Map<AdmissionReviewStatus, Set<AdmissionReviewStatus>> REVIEW_MOVES =
            new EnumMap<>(AdmissionReviewStatus.class);

    /** The fields that are not the status, so a body carrying none of them is a no-op. */
    private static final String NOTHING_MOVED =
            "Send a status, a score, a recommendation, criterion scores or notes.";

    /**
     * The fields #28 may be ordered by: what a caller types -> the field on the document.
     *
     * <p><b>An allowlist is a security control, not a convenience</b> — ordering is a read, and
     * sorting by a field walks its values out of the database a page at a time.
     *
     * <p>{@code notes} and {@code criterionScores} are deliberately absent: Mongo would sort them
     * by their first element, which means nothing, and both carry what a reviewer wrote about a
     * child.
     */
    private static final Map<String, String> SORTABLE_REVIEW_FIELDS = new LinkedHashMap<>();

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_REVIEW_FIELD_NAMES;

    /**
     * The default order: soonest due first, then by id.
     *
     * <p><b>A queue is sorted by when things are due</b>, which is the whole of what #28 is for.
     *
     * <p><b>{@code id} is the tiebreaker, and it has to be something.</b> A review has no unique
     * business key — its uniqueness is the triple of application, round and reviewer — so the
     * document id is the only total order available. Without it, two reviews sharing a due date
     * can swap places between pages and one row is shown twice while another is never shown.
     *
     * <p><b>A review with no due date sorts FIRST</b>, because Mongo puts a missing field before
     * every value in an ascending sort. That is the wrong end of a queue and it is not worth an
     * aggregation to fix: {@code overdue=true} is the filter that answers "what is late", and it
     * excludes them.
     */
    private static final Sort REVIEW_ORDER =
            Sort.by(Sort.Order.asc("dueAt"), Sort.Order.asc("id"));

    static {
        REVIEW_MOVES.put(AdmissionReviewStatus.PENDING, EnumSet.of(
                AdmissionReviewStatus.IN_PROGRESS, AdmissionReviewStatus.COMPLETED,
                AdmissionReviewStatus.CANCELLED));
        REVIEW_MOVES.put(AdmissionReviewStatus.IN_PROGRESS, EnumSet.of(
                AdmissionReviewStatus.COMPLETED, AdmissionReviewStatus.CANCELLED));
        //! TERMINAL, and spelled out rather than left missing. An absent key and an empty set mean
        //! the same thing to the code, but only one of them says it was decided.
        REVIEW_MOVES.put(AdmissionReviewStatus.COMPLETED,
                EnumSet.noneOf(AdmissionReviewStatus.class));
        REVIEW_MOVES.put(AdmissionReviewStatus.CANCELLED,
                EnumSet.noneOf(AdmissionReviewStatus.class));

        SORTABLE_REVIEW_FIELDS.put("dueat", "dueAt");
        SORTABLE_REVIEW_FIELDS.put("completedat", "completedAt");
        SORTABLE_REVIEW_FIELDS.put("reviewround", "reviewRound");
        SORTABLE_REVIEW_FIELDS.put("status", "status");
        SORTABLE_REVIEW_FIELDS.put("score", "score");
        SORTABLE_REVIEW_FIELDS.put("createdat", "createdAt");
        SORTABLE_REVIEW_FIELDS.put("updatedat", "updatedAt");
        SORTABLE_REVIEW_FIELD_NAMES =
                String.join(", ", SORTABLE_REVIEW_FIELDS.values());
    }

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

    /**
     * Endpoint #27 — what the reviewer found.
     *
     * <p><b>Only what you send moves.</b> Every field is optional, so a reviewer can save a score
     * today and add the recommendation tomorrow — and a body carrying nothing is a {@code 400}
     * rather than a silent 200.
     *
     * <p><b>Moving it to {@code COMPLETED} is the completion</b>, and that is when
     * {@code completedAt} is stamped. There is no separate "finish" verb: the status is named
     * directly, as #3 and #20 do.
     *
     * <p><b>A finished review cannot be edited.</b> Both {@code COMPLETED} and {@code CANCELLED}
     * are terminal — a score typed wrong is corrected by cancelling and assigning another, which
     * leaves both in the history rather than overwriting one.
     */
    public AdmissionReviewResponse recordResult(String admissionReviewId,
            AdmissionReviewUpdateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String id = admissionReviewId == null ? "" : admissionReviewId.trim();
        log.info("[recordResult] Step 1: Recording on review {} for school {}",
                id, school.getId());

        //! step 2 - the review, scoped by school in the QUERY. An id from another school is a real
        //! id, and a score written against it would be this school's mark on another school's
        //! child.
        // TODO: read admission review
        AdmissionReview review = admissionReviews.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("REVIEW_NOT_FOUND",
                        "No admission review with id '" + id + "' in this school."));

        //! step 3 - a finished review is a record, not a draft.
        if (review.getStatus() == AdmissionReviewStatus.COMPLETED) {
            throw ApiException.conflict("REVIEW_ALREADY_COMPLETED",
                    "That review was completed on " + review.getCompletedAt() + " and cannot be "
                            + "changed. A score recorded wrongly is corrected by cancelling this "
                            + "review and assigning another — which leaves both in the history.");
        }
        if (review.getStatus() == AdmissionReviewStatus.CANCELLED) {
            throw ApiException.conflict("REVIEW_CANCELLED",
                    "That review was cancelled and cannot be changed. Assign another with #26.");
        }

        //! step 4 - is the body carrying anything at all.
        //!
        //! FIRST, BEFORE THE VERSION AND BEFORE THE STATUS. It is the only check about the REQUEST
        //! rather than about the world, and a body that asks for nothing is meaningless whatever
        //! state the review is in. Asked second, `{"version": 5}` on its own answered
        //! CONCURRENT_MODIFICATION — true, and no help at all to somebody who sent an empty body.
        boolean movesSomething = request.status() != null || request.score() != null
                || request.recommendation() != null || request.criterionScores() != null
                || request.notes() != null;

        if (!movesSomething) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "This request changes nothing. " + NOTHING_MOVED);
        }

        //! step 5 - somebody else may have recorded on it while this caller was reading.
        if (request.version() != null && !request.version().equals(review.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "That review changed since you read it — it is " + review.getStatus()
                            + " now. Read it again before recording, so you are not overwriting "
                            + "what somebody else put there.");
        }

        //! step 6 - is the move one the review can make from where it is.
        AdmissionReviewStatus from = review.getStatus();
        if (request.status() != null && request.status() != from) {
            Set<AdmissionReviewStatus> allowed = REVIEW_MOVES.getOrDefault(
                    from, EnumSet.noneOf(AdmissionReviewStatus.class));

            if (!allowed.contains(request.status())) {
                throw ApiException.conflict("INVALID_REVIEW_TRANSITION",
                        "That review is " + from + ", so it cannot be moved to " + request.status()
                                + ". From here it can go to: " + names(allowed) + ".");
            }
        }

        //! step 7 - a finished review has to say what it recommends, and a cancelled one why.
        String notes = TextHelper.blankToNull(request.notes());

        if (request.status() == AdmissionReviewStatus.COMPLETED
                && request.recommendation() == null && review.getRecommendation() == null) {
            throw ApiException.badRequest("RECOMMENDATION_REQUIRED",
                    "A completed review has to say what it recommends — APPROVE, REJECT, WAITLIST "
                            + "or REQUEST_MORE_INFORMATION. It is the one thing a review exists to "
                            + "produce.");
        }
        if (request.status() == AdmissionReviewStatus.CANCELLED
                && notes == null && review.getNotes() == null) {
            throw ApiException.badRequest("CANCELLATION_NOTE_REQUIRED",
                    "Cancelling a review needs a note saying why, the same as a lost inquiry or a "
                            + "refused application. Work called off with no reason is a gap in the "
                            + "record.");
        }

        //! step 8 - build the change. ONLY WHAT WAS SENT, so a score saved today survives a
        //! recommendation added tomorrow.
        if (request.score() != null) {
            review.setScore(request.score());
        }
        if (request.recommendation() != null) {
            review.setRecommendation(request.recommendation());
        }
        //! REPLACED WHOLE, not merged. A map is one value, and merging would leave no way to
        //! remove a criterion recorded by mistake. `{}` therefore clears it.
        if (request.criterionScores() != null) {
            review.setCriterionScores(new HashMap<>(request.criterionScores()));
        }
        //! "" CLEARS, which is the project's convention for a String and works here because a
        //! note has an empty form, unlike an Instant.
        if (request.notes() != null) {
            review.setNotes(notes);
        }

        if (request.status() != null) {
            review.setStatus(request.status());

            //! STAMPED ON THE WAY IN TO COMPLETED, and never on the way to anything else. A
            //! cancelled review was not completed, however much of it was filled in.
            if (request.status() == AdmissionReviewStatus.COMPLETED) {
                review.setCompletedAt(Instant.now());
            }
        }

        //! step 9 - save
        // TODO: update admission review
        AdmissionReview saved = admissionReviews.save(review);
        log.info("[recordResult] Step 2: Review {} moved {} -> {}",
                saved.getId(), from, saved.getStatus());

        //! step 10 - the names, for the answer. Read tolerantly: a reviewer who has left the
        //! school must not stop their review being recorded.
        // TODO: read staff
        String reviewerName = staff.findByIdAndSchoolId(saved.getReviewerDocsId(), school.getId())
                .map(Staff::getFullName)
                .orElse(null);

        // TODO: read admission application
        String applicationNo = applications
                .findByIdAndSchoolId(saved.getAdmissionApplicationDocsId(), school.getId())
                .map(AdmissionApplication::getApplicationNo)
                .orElse(null);

        return AdmissionReviewResponse.fromReview(saved, applicationNo, reviewerName,
                nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #28 — a reviewer's queue.
     *
     * <p><b>Soonest due first</b>, which is the whole of what a queue is. Filter by reviewer and
     * status and you have one person's outstanding work — the shape
     * {@code school_reviewer_status_due_idx} was built for.
     *
     * <p><b>No gate runs.</b> A read, so a suspended school can still see what it owes.
     */
    public PageResponse<AdmissionReviewSummaryResponse> listReviews(
            AdmissionReviewSearchRequest request) {

        //! step 1 - paging and sorting first, so a bad sort is refused before anything is read.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_REVIEW_FIELDS, SORTABLE_REVIEW_FIELD_NAMES, REVIEW_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: this is a read.
        School school = currentSchool.require();

        //! step 3 - the search. The school id is passed in and never taken from the request.
        // TODO: search admission reviews
        Page<AdmissionReview> found = admissionReviews.search(school.getId(), request, pageable);
        log.info("[listReviews] Step 1: Found {} review(s) in total", found.getTotalElements());

        //! step 4 - the names, ONE QUERY EACH for the whole page rather than one per row. A queue
        //! of raw ids is not a queue anybody can work from.
        List<String> reviewerIds = found.getContent().stream()
                .map(AdmissionReview::getReviewerDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        List<String> applicationIds = found.getContent().stream()
                .map(AdmissionReview::getAdmissionApplicationDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        //! NOTHING TO LOOK UP IS NOT A QUERY. An empty page is the common case on a filter that
        //! matches nothing.
        // TODO: read staff
        Map<String, String> reviewerNames = reviewerIds.isEmpty() ? Map.of()
                : staff.findBySchoolIdAndIdIn(school.getId(), reviewerIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                                (first, second) -> first));

        // TODO: read admission applications
        List<AdmissionApplication> forms = applicationIds.isEmpty() ? List.of()
                : applications.findBySchoolIdAndIdIn(school.getId(), applicationIds);

        Map<String, AdmissionApplication> formsById = forms.stream().collect(Collectors.toMap(
                AdmissionApplication::getId, form -> form, (first, second) -> first));

        //! step 5 - thin rows. The criterion scores and the notes are on the review itself.
        return PageResponse.from(found, review -> {
            AdmissionApplication form = formsById.get(review.getAdmissionApplicationDocsId());
            return AdmissionReviewSummaryResponse.fromReview(review,
                    form == null ? null : form.getApplicationNo(),
                    form == null ? null : form.getApplicantName(),
                    reviewerNames.get(review.getReviewerDocsId()));
        });
    }

    /**
     * What can be done to this review next, in plain words.
     *
     * <p>Inline as a private method: used by one endpoint, and the folder rules keep single-use
     * logic where it is used.
     */
    private static String nextStepFor(AdmissionReview review) {
        return switch (review.getStatus()) {
            case PENDING -> "Nobody has started it. #27 is what records what they found, and "
                    + "moving it to COMPLETED is the completion.";
            case IN_PROGRESS -> "It is being worked on. Moving it to COMPLETED needs a "
                    + "recommendation, which is the one thing a review exists to produce.";
            case COMPLETED -> "It is done and can no longer be changed. #20 is what turns "
                    + "recommendations into the school's decision — it does not read this review, "
                    + "and does not have to.";
            case CANCELLED -> "The school called it off. Assign another with #26 if somebody still "
                    + "needs to look.";
        };
    }

    /** The reachable statuses as a sentence, so a refusal can list them. Used by: recordResult(). */
    private static String names(Set<AdmissionReviewStatus> allowed) {
        return allowed.isEmpty() ? "nothing"
                : allowed.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }
}
