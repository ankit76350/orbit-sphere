package com.orbitastra.backend.services.crm;

import java.time.Instant;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationAssignRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationCreateRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationWithdrawRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationDecisionRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationSearchRequest;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationDetailResponse;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationResponse;
import com.orbitastra.backend.dto.crm.admissionapplication.response.AdmissionApplicationSummaryResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.AdmissionReview;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissionoffer.AdmissionOfferRepository;
import com.orbitastra.backend.repositories.crm.admissionreview.AdmissionReviewRepository;
import com.orbitastra.backend.repositories.crm.inquiry.InquiryRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.crm.helper.CrmHelper;
import com.orbitastra.backend.services.crm.utils.AdmissionApplicationServiceUtils;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admission applications — the form a family fills in. Endpoints #17, #19, #20, #24 and #25 of the plan in
 * {@code controllers/crm/README.md}; only those five are built.
 *
 * <p><b>This is the first thing in the module that needs a cycle to be OPEN</b>, which is what #3
 * made possible. Before it, no cycle could leave DRAFT and nothing could be applied to.
 *
 * <p><b>An application does not need an inquiry.</b> The family that walks in with a completed
 * form never enquired, so {@code inquiryDocsId} is nullable — and that is why the whole pipeline
 * is testable without a single lead in the database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdmissionApplicationService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * How many answers a form may carry.
     *
     * <p><b>Nothing validates the answers themselves</b> — there is no form definition to check
     * them against — so the only thing that can be bounded is how many there are. Without this the
     * field is an unbounded map a caller controls.
     */
    private static final int MAX_FORM_ANSWERS = 200;

    /**
     * The fields #24 may be ordered by: what a caller types -> the field on the document.
     *
     * <p><b>A security control, not a convenience.</b> Ordering is a read: sort by a field and
     * walk the pages and you learn its values even when nothing displays them. That matters more
     * here than on the cycle list — an application carries a child's date of birth.
     *
     * <p>{@code guardians}, {@code formAnswers} and {@code evidenceDocumentDocsIds} are all absent
     * on purpose: Mongo sorts an array or a map by its first element, which would order children
     * by whichever parent happened to be typed first.
     */
    private static final Map<String, String> SORTABLE_APPLICATION_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_APPLICATION_FIELDS.put("applicationno", "applicationNo");
        SORTABLE_APPLICATION_FIELDS.put("applicantname", "applicantName");
        SORTABLE_APPLICATION_FIELDS.put("status", "status");
        SORTABLE_APPLICATION_FIELDS.put("submittedat", "submittedAt");
        SORTABLE_APPLICATION_FIELDS.put("createdat", "createdAt");
        SORTABLE_APPLICATION_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_APPLICATION_FIELD_NAMES =
            SORTABLE_APPLICATION_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order: newest form first, then by its number.
     *
     * <p><b>It is also the tiebreaker on every other sort</b> — {@link PageResponse#pageableOf}
     * appends the fallback to whatever the caller named. {@code applicationNo} is unique within a
     * school ({@code school_application_no_uniq}), so every sort ends in a total order and paging
     * cannot show one row twice while never showing another.
     *
     * <p><b>Not {@code submittedAt}</b>, which would look like the obvious choice: a DRAFT has
     * none, so every unsubmitted form would sort together in an order nothing decides.
     */
    private static final Sort APPLICATION_ORDER =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("applicationNo"));

    /**
     * What #20 may move an application to, from where. Mirrors {@code CYCLE_MOVES} on #3.
     *
     * <p><b>This table is the endpoint.</b> Everything else it does is reading, checking and
     * saving; this is the product rule — and it is the whole guard, which is why the request names
     * a status directly rather than through a parallel decision vocabulary.
     *
     * <p><b>{@code SUBMITTED} is in it, which the drawn graph does not show.</b> The plan says #20
     * does not require a completed review — small schools decide in a conversation — and insisting
     * on {@code UNDER_REVIEW} would mean assigning a reviewer first, which <i>is</i> inventing a
     * review row. So a form can go straight from sent to decided.
     *
     * <p><b>{@code APPROVED} is terminal HERE, though not in the module.</b> Once a school has
     * approved somebody the next thing that happens is an offer (#29); changing its mind is
     * withdrawing that offer (#31), not deciding again. Leaving it empty keeps one answer to "what
     * happened to this child".
     *
     * <p><b>{@code WAITLISTED} allows only {@code APPROVED} and {@code REJECTED}.</b> Waitlisting
     * something already waitlisted moves nothing, and asking a waitlisted family for more
     * information is a case nobody has described.
     *
     * <p><b>Every status is spelled out, including the ones that can go nowhere.</b> An absent key
     * and an empty set mean the same thing to the code, but only one of them says it was decided —
     * the same call #3 made.
     */
    private static final Map<AdmissionApplicationStatus, Set<AdmissionApplicationStatus>>
            DECISION_MOVES = new EnumMap<>(AdmissionApplicationStatus.class);

    /**
     * The moves that will not be made without a reason.
     *
     * <p>{@code REJECTED} is the plan's. {@code ADDITIONAL_INFORMATION_REQUIRED} was added when
     * this was built: asking a family for more without saying what tells them nothing.
     */
    private static final Set<AdmissionApplicationStatus> NEEDS_A_NOTE = EnumSet.of(
            AdmissionApplicationStatus.REJECTED,
            AdmissionApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED);

    static {
        //! DECIDED WITHOUT ANYBODY REVIEWING IT. The conversation-in-the-corridor case.
        DECISION_MOVES.put(AdmissionApplicationStatus.SUBMITTED, EnumSet.of(
                AdmissionApplicationStatus.APPROVED, AdmissionApplicationStatus.REJECTED,
                AdmissionApplicationStatus.WAITLISTED,
                AdmissionApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED));

        DECISION_MOVES.put(AdmissionApplicationStatus.UNDER_REVIEW, EnumSet.of(
                AdmissionApplicationStatus.APPROVED, AdmissionApplicationStatus.REJECTED,
                AdmissionApplicationStatus.WAITLISTED,
                AdmissionApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED));

        //! THE ONLY PLACE UNDER_REVIEW CAN BE ASKED FOR, and the edge #26 deliberately left to
        //! #20: assigning another reviewer is not what decides the information turned up. The
        //! school can also just decide, if what arrived settled it.
        DECISION_MOVES.put(AdmissionApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED, EnumSet.of(
                AdmissionApplicationStatus.UNDER_REVIEW, AdmissionApplicationStatus.APPROVED,
                AdmissionApplicationStatus.REJECTED, AdmissionApplicationStatus.WAITLISTED));

        //! A SEAT CAME FREE, or the school finally said no.
        DECISION_MOVES.put(AdmissionApplicationStatus.WAITLISTED, EnumSet.of(
                AdmissionApplicationStatus.APPROVED, AdmissionApplicationStatus.REJECTED));

        //! NOTHING THIS ENDPOINT CAN DO, spelled out rather than left missing.
        for (AdmissionApplicationStatus nothing : EnumSet.of(
                AdmissionApplicationStatus.DRAFT, AdmissionApplicationStatus.APPROVED,
                AdmissionApplicationStatus.REJECTED, AdmissionApplicationStatus.WITHDRAWN,
                AdmissionApplicationStatus.OFFERED, AdmissionApplicationStatus.OFFER_ACCEPTED,
                AdmissionApplicationStatus.ENROLLED)) {
            DECISION_MOVES.put(nothing, EnumSet.noneOf(AdmissionApplicationStatus.class));
        }
    }

    private final AdmissionApplicationRepository applications;
    private final InquiryRepository inquiries;
    private final AdmissionReviewRepository admissionReviews;
    private final AdmissionOfferRepository admissionOffers;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final CrmHelper helper;
    private final AdmissionApplicationServiceUtils utils;

    /**
     * Endpoint #17 — starts an application against an open cycle.
     *
     * <p>Creates in {@code DRAFT}. Submitting it is #19, which is not built, so nothing can move
     * past DRAFT yet.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND   no cycle with that id in this school
     * 409 CYCLE_NOT_OPEN              the cycle is not taking applications
     * 404 INQUIRY_NOT_FOUND           an inquiry that is not this school's
     * 409 APPLICATION_ALREADY_EXISTS  that inquiry already applied to that cycle
     * 409 CLASS_NOT_IN_CYCLE_YEAR     the class is not of the cycle's year
     * 409 CLASS_NOT_IN_CAPACITY       the cycle's seat table does not list that class
     * 400 TOO_MANY_FORM_ANSWERS       more answers than the cap
     * </pre>
     */
    public AdmissionApplicationResponse createApplication(
            AdmissionApplicationCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[createApplication] Step 1: Checking the cycle is open");

        //! step 2 - the cycle, and it has to be taking applications. THIS IS THE MODULE'S GATE 4:
        //! every other module asks "is this the running year"; admissions asks "is this cycle
        //! open", because a cycle for a year that has not started is the normal case here.
        AdmissionCycle cycle = helper.loadOpenCycle(school, request.admissionCycleDocsId());

        //! step 3 - the class has to be one of the CYCLE'S year, not merely of the school.
        String classId = request.appliedClassDocsId().trim();
        log.info("[createApplication] Step 2: Checking class {} belongs to '{}'",
                classId, cycle.getAcademicYear());

        // TODO: read school class
        SchoolClass applied = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(classId, school.getId(),
                        cycle.getAcademicYear())
                .orElseThrow(() -> ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                        "Class '" + classId + "' is not a class of '" + cycle.getAcademicYear()
                                + "', which is the year this cycle admits into."));

        //! step 4 - and it has to have seats. #3 refuses to open a cycle with an empty table, but
        //! a table can list some classes and not others, and applying for a class with no seats is
        //! an application that could never be offered anything.
        List<IntakeCapacity> seats = cycle.getCapacities() == null
                ? List.of()
                : cycle.getCapacities();
        boolean hasSeats = seats.stream()
                .anyMatch(seat -> classId.equals(seat.getClassDocsId()));
        if (!hasSeats) {
            throw ApiException.conflict("CLASS_NOT_IN_CAPACITY",
                    "'" + cycle.getName() + "' has no seats set up for " + applied.getName()
                            + ". A class that is not in the seat table cannot be applied for — "
                            + "add it with the seat table endpoint first.");
        }

        //! step 5 - the inquiry, when one was named. Optional: a family that walks in with a
        //! completed form never enquired, and refusing them would be refusing the common case.
        String inquiryId = TextHelper.blankToNull(request.inquiryDocsId());
        Inquiry inquiry = null;
        if (inquiryId != null) {
            log.info("[createApplication] Step 3: Reading inquiry {}", inquiryId);

            // TODO: read inquiry
            inquiry = inquiries.findByIdAndSchoolId(inquiryId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND",
                            "No inquiry with id '" + inquiryId + "' in this school."));

            //! ONE INQUIRY, ONE APPLICATION PER CYCLE. school_cycle_inquiry_uniq enforces it and
            //! would otherwise fire as a duplicate key error the handler turns into a 500. An
            //! inquiry is about ONE prospective child - it carries their name and date of birth -
            //! so two applications from it in one round is two applications for one child.
            // TODO: check admission application exists
            if (applications.existsBySchoolIdAndAdmissionCycleDocsIdAndInquiryDocsId(
                    school.getId(), cycle.getId(), inquiryId)) {
                throw ApiException.conflict("APPLICATION_ALREADY_EXISTS",
                        "That inquiry has already produced an application in '" + cycle.getName()
                                + "'. One inquiry is one child applying once per round — a second "
                                + "child needs their own inquiry.");
            }
        }

        //! step 6 - the guardians. Copied from the inquiry when there is one, then OVERRIDDEN by
        //! whatever the form carried: the parent filling it in is the one who signs, and editing
        //! the inquiry afterwards must not rewrite an application the school has acted on.
        List<InquiryGuardian> guardians = new ArrayList<>();
        for (AdmissionApplicationCreateRequest.Guardian sent : request.guardians()) {
            InquiryGuardian one = InquiryGuardian.builder()
                    .fullName(sent.fullName().trim())
                    .relation(sent.relation())
                    .phoneNumber(TextHelper.blankToNull(sent.phoneNumber()))
                    .emailAddress(TextHelper.blankToNull(sent.emailAddress()))
                    .address(TextHelper.blankToNull(sent.address()))
                    .occupation(TextHelper.blankToNull(sent.occupation()))
                    .primaryContact(sent.primaryContact() != null && sent.primaryContact())
                    .build();
            guardians.add(one);
        }

        //! step 7 - the answers. Nothing checks WHAT they are, so the only thing that can be
        //! checked is how many, which stops the field being an unbounded map a caller controls.
        var answers = request.formAnswers() == null
                ? new HashMap<String, Object>()
                : new HashMap<>(request.formAnswers());
        if (answers.size() > MAX_FORM_ANSWERS) {
            throw ApiException.badRequest("TOO_MANY_FORM_ANSWERS",
                    "That form carries " + answers.size() + " answers and the limit is "
                            + MAX_FORM_ANSWERS + ".");
        }

        //! step 8 - the number. Generated, never supplied: nobody picks their own application
        //! number, and a caller-supplied one lets two families collide.
        String applicationNo = numberSequences.next(school.getId(),
                NumberSequenceType.ADMISSION_APPLICATION, "APP/{YYYY}/{MM}/");
        log.info("[createApplication] Step 4: Allocated application number {}", applicationNo);

        //! step 9 - build it. schoolId set by hand: nothing fills it in, and a row without it
        //! belongs to no school and is invisible to every read.
        AdmissionApplication application = AdmissionApplication.builder()
                .schoolId(school.getId())
                .applicationNo(applicationNo)
                .admissionCycleDocsId(cycle.getId())
                .inquiryDocsId(inquiryId)
                .appliedClassDocsId(classId)
                .applicantName(request.applicantName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .guardians(guardians)
                .status(AdmissionApplicationStatus.DRAFT)
                .formAnswers(answers)
                .evidenceDocumentDocsIds(new ArrayList<>())
                .build();

        //! step 10 - save
        // TODO: insert admission application
        AdmissionApplication saved = applications.save(application);
        log.info("[createApplication] Step 5: Saved application {} as a DRAFT", saved.getId());

        //! step 11 - the lead, if there was one, has started an application.
        //! Done AFTER the application is saved, not before: if the insert fails, an inquiry that
        //! says APPLICATION_STARTED with no application is a worse lie than one that is late.
        if (inquiry != null) {
            inquiry.setStatus(InquiryStatus.APPLICATION_STARTED);

            // TODO: update inquiry
            inquiries.save(inquiry);
            log.info("[createApplication] Step 6: Moved inquiry {} to APPLICATION_STARTED",
                    inquiry.getId());
        }

        return AdmissionApplicationResponse.fromApplication(saved, applied.getName(),
                "'" + saved.getApplicantName() + "' has a DRAFT application for "
                        + applied.getName() + ". Submitting it is #19, which is not built, so it "
                        + "cannot move past DRAFT yet. " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #24 — one page of this school's applications. <b>The pipeline.</b>
     *
     * <p>Filter by cycle, class, status and officer — which is the order
     * {@code school_cycle_class_status_idx} is built in, and the worklist an admission officer
     * actually opens. Search the applicant's name or the application number. Every filter is
     * optional.
     *
     * <pre>
     * 400 INVALID_PAGE           a negative page
     * 400 INVALID_PAGE_SIZE      a size below 1 or above the cap
     * 400 INVALID_SORT_FIELD     a field that is not in the allowlist
     * </pre>
     *
     * <p><b>No gates.</b> Reads run none — a suspended school still sees who applied to it.
     */
    public PageResponse<AdmissionApplicationSummaryResponse> listApplications(
            AdmissionApplicationSearchRequest request) {

        //! step 1 - the paging and the order, checked before anything is read. Cheap checks with
        //! no database behind them go first, so a bad sort costs no round trip.
        log.info("[listApplications] Step 1: Checking the paging and the sort order");
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_APPLICATION_FIELDS, SORTABLE_APPLICATION_FIELD_NAMES, APPLICATION_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: this is a read, and a school
        //! that cannot be edited can still look at its own pipeline.
        School school = currentSchool.require();

        //! step 3 - the search. The school id is passed in and never taken from the request.
        // TODO: search admission applications
        var found = applications.search(school.getId(), request, pageable);
        log.info("[listApplications] Step 2: Found {} application(s) in total",
                found.getTotalElements());

        //! step 4 - the officers' names, ONE QUERY FOR THE WHOLE PAGE rather than one per row.
        //!
        //! WRITTEN WHEN #22 ARRIVED, and not before: until something could assign an officer this
        //! branch had nothing to resolve and no way to be tested. A worklist that can be FILTERED
        //! by officer but only ever shows raw ids is not a worklist anybody can work from.
        List<String> officerIds = found.getContent().stream()
                .map(AdmissionApplication::getAssignedAdmissionOfficerDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        //! NOTHING TO LOOK UP IS NOT A QUERY. A page of unassigned forms is the common case.
        // TODO: read staff
        Map<String, String> officerNames = officerIds.isEmpty()
                ? Map.of()
                : staff.findBySchoolIdAndIdIn(school.getId(), officerIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                                (first, second) -> first));

        //! step 5 - thin rows. The guardians, the answers and the evidence are on #25.
        return PageResponse.from(found, one -> AdmissionApplicationSummaryResponse.fromApplication(
                one, one.getAssignedAdmissionOfficerDocsId() == null ? null
                        : officerNames.get(one.getAssignedAdmissionOfficerDocsId())));
    }

    /**
     * Endpoint #19 — the family submits the form, and the snapshot freezes.
     *
     * <p><b>This is the line the module is built around.</b> Before it, the applicant and guardian
     * fields are a draft the family is still filling in. After it, they are a record of what the
     * family actually declared — and #18, which edits a form, refuses from here on. A school that
     * could rewrite those afterwards could not answer "what did they actually tell us".
     *
     * <p><b>It asks the cycle the same two questions #17 does</b>, and for the same reason: the
     * status says whether anybody opened the round, and the published dates say what the school
     * promised families. A form started an hour before the deadline and submitted an hour after it
     * is a late application, and the whole point of the window is that it is the moment of
     * <i>submission</i> that counts.
     *
     * <p><b>It does NOT re-check the seat table.</b> #17 refuses a class with no seats, and the
     * school can empty that table afterwards with #4 — but submitting is the family's act, and
     * refusing it because the school changed its own plan would punish the wrong side. Capacity is
     * decided when a seat is offered, which is #29, and counted by #7. The dates are the calendar;
     * the seat table is not.
     */
    public AdmissionApplicationResponse submitApplication(String admissionApplicationId) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[submitApplication] Step 1: Submitting application {} for school {}",
                admissionApplicationId, school.getId());

        //! step 2 - the form, scoped by school in the QUERY. An id from another school is a real
        //! id, and submitting somebody else's form is worse than reading it.
        AdmissionApplication application = utils.loadApplication(school, admissionApplicationId);

        //! step 3 - only a DRAFT can be submitted, and this is checked BEFORE the cycle. Somebody
        //! pressing submit twice should be told the form is already in, not that the round has
        //! since closed — the second message is true and completely unhelpful.
        if (application.getStatus() != AdmissionApplicationStatus.DRAFT) {
            throw ApiException.conflict("INVALID_APPLICATION_TRANSITION",
                    "'" + application.getApplicantName() + "' is "
                            + application.getStatus() + ", and only a DRAFT can be submitted. "
                            + (application.getStatus() == AdmissionApplicationStatus.SUBMITTED
                                    ? "This form is already in — submitting it again would "
                                            + "overwrite the moment the family sent it."
                                    : "It has already moved past the point where the family "
                                            + "could send it."));
        }

        //! step 4 - the round has to still be taking forms. THE SAME CHECK #17 MAKES, and the
        //! moment that counts is now rather than when the draft was started.
        AdmissionCycle cycle = helper.loadOpenCycle(school, application.getAdmissionCycleDocsId());
        log.info("[submitApplication] Step 2: '{}' is open and inside its window", cycle.getName());

        //! step 5 - the class name, for the answer. Read tolerantly: a class that is gone must not
        //! stop a family submitting a form that was valid when they started it.
        String appliedClassName = utils.classNameOrNull(school,
                application.getAppliedClassDocsId(), cycle.getAcademicYear());

        //! step 6 - build the change
        application.setStatus(AdmissionApplicationStatus.SUBMITTED);
        application.setSubmittedAt(Instant.now());

        //! step 7 - save
        // TODO: update admission application
        AdmissionApplication saved = applications.save(application);
        log.info("[submitApplication] Step 3: Application {} is SUBMITTED", saved.getId());

        //! step 8 - the lead, when the form came from one, has now been submitted.
        //!
        //! READ TOLERANTLY, and skipped when the lead is gone. #17 refuses an inquiry it cannot
        //! find, which is right when the family is naming one — but here the link was checked
        //! months ago, and a lead somebody deleted since must not be able to stop a family
        //! submitting their application. The form is the thing that matters; the lead is a note
        //! about how it arrived.
        if (application.getInquiryDocsId() != null) {
            // TODO: read inquiry
            Optional<Inquiry> lead = inquiries.findByIdAndSchoolId(
                    application.getInquiryDocsId(), school.getId());

            if (lead.isPresent()) {
                Inquiry inquiry = lead.get();
                inquiry.setStatus(InquiryStatus.APPLICATION_SUBMITTED);

                // TODO: update inquiry
                inquiries.save(inquiry);
                log.info("[submitApplication] Step 4: Moved inquiry {} to APPLICATION_SUBMITTED",
                        inquiry.getId());
            } else {
                log.warn("[submitApplication] Step 4: Application {} names inquiry {}, which is "
                        + "not in this school any more. Submitted anyway.",
                        saved.getId(), application.getInquiryDocsId());
            }
        }

        return AdmissionApplicationResponse.fromApplication(saved, appliedClassName,
                "'" + saved.getApplicantName() + "' is SUBMITTED, and the form is now frozen — "
                        + "#18 refuses to edit it from here. Next is a review (#26) or a decision "
                        + "(#20); neither is built. " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #20 — what the school decided.
     *
     * <p><b>It does not need a review to exist.</b> Small schools decide in a conversation, and an
     * endpoint that insisted on one would make them invent it — so a {@code SUBMITTED} form can be
     * decided without ever having been assigned to anybody.
     *
     * <p><b>The caller says what they are doing, not what the status should become.</b> A body that
     * named the status would let somebody write {@code ENROLLED} onto a form nobody had offered a
     * seat to.
     */
    public AdmissionApplicationResponse decide(String admissionApplicationId,
            AdmissionApplicationDecisionRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[decide] Step 1: Moving application {} to {} for school {}",
                admissionApplicationId, request.status(), school.getId());

        //! step 2 - the form, scoped by school in the QUERY.
        AdmissionApplication application = utils.loadApplication(school, admissionApplicationId);

        //! step 3 - somebody else may have decided it while this caller was reading.
        if (request.version() != null && !request.version().equals(application.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + application.getApplicantName() + "' changed since you read it — it is "
                            + application.getStatus() + " now. Read it again before deciding, so "
                            + "you are not deciding something somebody has already settled.");
        }

        //! step 4 - is this a move the form can make from where it is.
        Set<AdmissionApplicationStatus> allowed = DECISION_MOVES.getOrDefault(
                application.getStatus(), EnumSet.noneOf(AdmissionApplicationStatus.class));

        if (!allowed.contains(request.status())) {
            throw ApiException.conflict("INVALID_APPLICATION_TRANSITION",
                    "'" + application.getApplicantName() + "' is " + application.getStatus()
                            + ", so it cannot be moved to " + request.status() + ". "
                            + (allowed.isEmpty()
                                    ? describeWhyNothingIsAllowed(application.getStatus())
                                    : "From here the school can move it to: " + names(allowed)
                                            + "."));
        }

        //! step 5 - a refusal, and a request for more, both have to say why.
        String note = TextHelper.blankToNull(request.note());
        if (note == null && NEEDS_A_NOTE.contains(request.status())) {
            throw ApiException.badRequest("DECISION_NOTE_REQUIRED",
                    request.status() + " needs a note saying why. "
                            + (request.status() == AdmissionApplicationStatus.REJECTED
                                    ? "A refusal with no reason is the part of an admissions "
                                            + "record worth the most."
                                    : "Asking a family for more without saying what tells them "
                                            + "nothing."));
        }

        //! step 6 - a form cannot be APPROVED while somebody is still assessing it.
        //!
        //! ONLY APPROVED, and the asymmetry is the rule rather than an oversight. A school can
        //! REFUSE a form, waitlist it or ask the family for more without waiting for everybody —
        //! those are all answers a head can give over an incomplete picture, and asking for more
        //! information is often exactly WHY a review is still open. Admitting a child is the one
        //! decision that says every assessment was seen, so it is the one that has to be true.
        //!
        //! IT SKIPS THE CHECK WHEN NOTHING WAS ASSIGNED. A form nobody reviewed is decided in a
        //! conversation, which #20 has allowed since it was built — see its README entry. Having
        //! no reviews and having all of them finished are the same answer to this question.
        if (request.status() == AdmissionApplicationStatus.APPROVED) {
            // TODO: read admission reviews
            List<AdmissionReview> outstanding = admissionReviews
                    .findBySchoolIdAndAdmissionApplicationDocsIdAndStatusInOrderByReviewRoundAscCreatedAtAsc(
                            school.getId(), application.getId(), STILL_ASSESSING);

            if (!outstanding.isEmpty()) {
                //! THE ROUNDS, NOT A COUNT. "Two are outstanding" sends somebody hunting; naming
                //! the rounds and where each one has got to tells them what to chase.
                String stillOpen = outstanding.stream()
                        .map(one -> "round " + one.getReviewRound() + " (" + one.getStatus() + ")")
                        .collect(Collectors.joining(", "));

                throw ApiException.conflict("REVIEWS_STILL_OUTSTANDING",
                        "'" + application.getApplicantName() + "' cannot be approved while "
                                + outstanding.size() + " review"
                                + (outstanding.size() == 1 ? " is" : "s are")
                                + " still open: " + stillOpen + ". Finish each one with #27c, or "
                                + "call it off with #27d if nobody is going to — a cancelled "
                                + "review does not hold an approval up. Refusing, waitlisting or "
                                + "asking for more is still allowed from here.");
            }
        }

        //! step 7 - build the change
        AdmissionApplicationStatus from = application.getStatus();
        application.setStatus(request.status());
        application.setDecidedAt(Instant.now());

        //! A NOTE IS KEPT WHEN ONE IS SENT, and the old one is left alone when none is. A school
        //! resuming a review has not un-said why it asked for more.
        if (note != null) {
            application.setDecisionNote(note);
        }

        //! step 8 - save
        // TODO: update admission application
        AdmissionApplication saved = applications.save(application);
        log.info("[decide] Step 2: Application {} moved {} -> {}",
                saved.getId(), from, saved.getStatus());

        //! step 9 - the class name, for the answer. Both reads are tolerant: a round or a class
        //! that is gone must not stop a school recording what it decided.
        String academicYear = utils
                .loadCycleOrEmpty(school, application.getAdmissionCycleDocsId())
                .map(AdmissionCycle::getAcademicYear)
                .orElse(null);

        String appliedClassName = utils.classNameOrNull(school,
                application.getAppliedClassDocsId(), academicYear);

        return AdmissionApplicationResponse.fromApplication(saved, appliedClassName,
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #21 — the family pulls out.
     *
     * <p><b>This is the family's act, not the school's.</b> #20 is where a school records what it
     * decided; this is where it records that the family stopped. They reach the same kind of
     * ending from opposite directions, and conflating them would lose which one happened — a
     * school that refused a child and a family that went elsewhere are very different numbers at
     * the end of a season.
     *
     * <p><b>From anywhere before {@code ENROLLED}.</b> A draft nobody sent, a form under review, an
     * approved applicant, one holding an offer — a family can walk away at any of them, and the
     * graph has said so since before any of this was built.
     *
     * <p><b>A reason is required</b>, and it is the part worth the most: they went to another
     * school, the fees were too high, they moved city. A withdrawal with nothing said teaches
     * nobody anything.
     *
     * <p><b>It touches nothing but the application</b>, which is the plan's own collection list —
     * and it has a consequence worth knowing. A form withdrawn while it holds a live offer leaves
     * that offer {@code ISSUED}, so #32's chase list will still show it and somebody will ring a
     * family that has already gone. The endpoint that says the offer is over is #30 with
     * {@code DECLINED}, or #31; doing both is two calls, and each one records a different fact.
     * Folding them together here would make #21 guess which of those two happened.
     */
    public AdmissionApplicationResponse withdrawApplication(String admissionApplicationId,
            AdmissionApplicationWithdrawRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[withdrawApplication] Step 1: Withdrawing application {} for school {}",
                admissionApplicationId, school.getId());

        //! step 2 - the form, scoped by school in the QUERY.
        AdmissionApplication application = utils.loadApplication(school, admissionApplicationId);

        //! step 3 - somebody else may have moved it while this caller was reading.
        if (request.version() != null && !request.version().equals(application.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + application.getApplicantName() + "' changed since you read it — it is "
                            + application.getStatus() + " now. Read it again before withdrawing, "
                            + "so you are not recording a family as gone after something else "
                            + "happened to the form.");
        }

        //! step 4 - a family can pull out of anything they have not finished.
        if (CANNOT_BE_WITHDRAWN.contains(application.getStatus())) {
            throw ApiException.conflict("INVALID_APPLICATION_TRANSITION",
                    "'" + application.getApplicantName() + "' is " + application.getStatus()
                            + ", so it cannot be withdrawn. "
                            + (application.getStatus() == AdmissionApplicationStatus.ENROLLED
                                    ? "The child is a student now — leaving the school is the "
                                            + "student module's business, and writing WITHDRAWN "
                                            + "here would leave a register entry pointing at a "
                                            + "form that says they never came."
                                    : "The family already pulled out on "
                                            + application.getWithdrawnAt() + ", and withdrawing "
                                            + "again would only overwrite what they said then."));
        }

        //! step 5 - build the change. THE FAMILY'S REASON, and it is kept rather than logged: it
        //! goes in withdrawalReason and NOT in decisionNote, which is the school's own word about
        //! what IT decided. Two facts, two fields.
        AdmissionApplicationStatus from = application.getStatus();
        application.setStatus(AdmissionApplicationStatus.WITHDRAWN);
        application.setWithdrawnAt(Instant.now());
        application.setWithdrawalReason(request.withdrawalReason().trim());

        //! NOT decidedAt. The school did not decide anything — the family left — and stamping it
        //! would make every "how long did we take to decide" count include the ones nobody
        //! decided.

        //! step 6 - save
        // TODO: update admission application
        AdmissionApplication saved = applications.save(application);
        log.info("[withdrawApplication] Step 2: Application {} moved {} -> WITHDRAWN",
                saved.getId(), from);

        //! step 7 - the class name, for the answer. Tolerant, as everywhere here.
        String academicYear = utils
                .loadCycleOrEmpty(school, saved.getAdmissionCycleDocsId())
                .map(AdmissionCycle::getAcademicYear)
                .orElse(null);

        String appliedClassName = utils.classNameOrNull(school,
                saved.getAppliedClassDocsId(), academicYear);

        return AdmissionApplicationResponse.fromApplication(saved, appliedClassName,
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #22 — whose form this is.
     *
     * <p><b>An admission officer owns the application; a reviewer assesses it.</b> They are
     * different jobs and this is the endpoint for the first one. The officer chases the missing
     * birth certificate, answers the family's calls and makes sure the form does not sit for three
     * weeks — which is why #24 can filter by them, and why that filter returned nothing for every
     * id until this existed.
     *
     * <p><b>It moves no status and stamps no date.</b> Assigning an owner is not a decision, and
     * this is the difference from #26: putting a form on a <i>reviewer's</i> desk moves it to
     * {@code UNDER_REVIEW} because assessment has started, but giving it to an officer says
     * nothing about where the form has got to.
     *
     * <p><b>Reassigning is the normal case, not an error.</b> People leave, go on holiday and
     * swap workloads. Assigning the same person twice is a quiet 200 as well — unlike #27b, which
     * refuses a second start. The two are different intents: "make sure this is on Anita's list"
     * is worth being idempotent, "pick up work nobody has" is a claim that two people cannot both
     * make.
     *
     * <p><b>There is no unassign</b>, because the plan has none and a form belonging to nobody is
     * the state this endpoint exists to get rid of. A school whose officer leaves gives the form
     * to somebody else.
     */
    public AdmissionApplicationResponse assignOfficer(String admissionApplicationId,
            AdmissionApplicationAssignRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[assignOfficer] Step 1: Assigning application {} to {} for school {}",
                admissionApplicationId, request.assignedAdmissionOfficerDocsId(), school.getId());

        //! step 2 - the form, scoped by school in the QUERY.
        AdmissionApplication application = utils.loadApplication(school, admissionApplicationId);

        //! step 3 - somebody else may have reassigned it while this caller was reading.
        if (request.version() != null && !request.version().equals(application.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + application.getApplicantName() + "' changed since you read it. Read it "
                            + "again before assigning, so you are not taking it off somebody it "
                            + "was just given to.");
        }

        //! step 4 - a finished form is nobody's work.
        if (WORK_IS_OVER.contains(application.getStatus())) {
            throw ApiException.conflict("APPLICATION_NOT_ASSIGNABLE",
                    "'" + application.getApplicantName() + "' is " + application.getStatus()
                            + ", so there is nothing left for an admission officer to do with it. "
                            + "A form is given to somebody so they can move it along, and this one "
                            + "has stopped.");
        }

        //! step 5 - the officer has to be this school's staff. READ rather than checked for
        //! existence, because the name is wanted on the answer and this is the read that has it —
        //! the same call #26 makes for a reviewer, and the reason the answer costs no extra query.
        String officerId = request.assignedAdmissionOfficerDocsId().trim();

        // TODO: read staff
        Staff officer = staff.findByIdAndSchoolId(officerId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                        "No staff member with id '" + officerId + "' in this school, so this "
                                + "application cannot be given to them."));

        //! step 6 - build the change. ONE FIELD. Not the status, not a date, not the decision.
        String previous = application.getAssignedAdmissionOfficerDocsId();
        application.setAssignedAdmissionOfficerDocsId(officer.getId());

        //! step 7 - save
        // TODO: update admission application
        AdmissionApplication saved = applications.save(application);
        log.info("[assignOfficer] Step 2: Application {} moved from officer {} to {}",
                saved.getId(), previous, saved.getAssignedAdmissionOfficerDocsId());

        //! step 8 - the class name, for the answer. Tolerant, as everywhere here: a round or a
        //! class that is gone must not stop a school saying whose form this is.
        String academicYear = utils
                .loadCycleOrEmpty(school, saved.getAdmissionCycleDocsId())
                .map(AdmissionCycle::getAcademicYear)
                .orElse(null);

        String appliedClassName = utils.classNameOrNull(school,
                saved.getAppliedClassDocsId(), academicYear);

        //! THE OFFICER'S NAME COMES FROM STEP 5, not a second query. It was read to refuse an id
        //! that is not this school's, and it is on hand.
        return AdmissionApplicationResponse.fromApplication(saved, appliedClassName,
                officer.getFullName(),
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Why a status can take no decision at all, in words rather than an empty list.
     *
     * <p>Inline as a private method rather than in the helper: used by one endpoint, and the folder
     * rules keep single-use logic where it is used.
     *
     * <p><b>"Nothing is allowed" is the answer somebody will hit most often by mistake</b>, and a
     * refusal that just said so would leave them guessing whether the endpoint or the form was
     * wrong.
     */
    private static String describeWhyNothingIsAllowed(AdmissionApplicationStatus status) {
        return switch (status) {
            case DRAFT -> "The family has not submitted it yet — #19 is what sends it, and there "
                    + "is nothing to decide until they do.";
            case APPROVED -> "It is already approved, so the next step is an offer (#29) rather "
                    + "than another decision. Changing your mind is withdrawing the offer (#31).";
            case REJECTED -> "It has been refused, and nothing moves from there.";
            case WITHDRAWN -> "The family pulled out, and nothing moves from there.";
            case OFFERED -> "An offer is out with the family — #30 records their answer.";
            case OFFER_ACCEPTED -> "They accepted. #33 turns the applicant into a student.";
            case ENROLLED -> "The child is a student now, and this application is history.";
            default -> "Nothing can be decided from there.";
        };
    }

    /**
     * The statuses a form can no longer be withdrawn from.
     *
     * <p><b>The graph says "anything before {@code ENROLLED}", so the short list is the
     * exceptions.</b> A child who is already a student is not an applicant any more — undoing that
     * is the {@code student} module's business, and #21 writing {@code WITHDRAWN} over it would
     * leave a register entry pointing at a form that says the family never came.
     *
     * <p><b>{@code WITHDRAWN} is here because it has already happened.</b> Withdrawing twice is not
     * a second event; it would only overwrite the first reason with a later one.
     *
     * <p><b>{@code DRAFT} is deliberately NOT here.</b> A form the family started and abandoned is
     * exactly the kind of thing a school wants recorded rather than left sitting — and it is the
     * one case where "the family pulled out" needs no other endpoint to have run first.
     *
     * <p>Used by {@code withdrawApplication()}.
     */
    private static final Set<AdmissionApplicationStatus> CANNOT_BE_WITHDRAWN = EnumSet.of(
            AdmissionApplicationStatus.ENROLLED,
            AdmissionApplicationStatus.WITHDRAWN);

    /**
     * The statuses a form can no longer be given to anybody, because the work is over.
     *
     * <p><b>Spelled as what is REFUSED rather than what is allowed</b>, and that is the honest way
     * round here: an admission officer owns a form from the moment it exists until it stops being
     * anybody's problem, so the short list is the exceptions.
     *
     * <p><b>{@code DRAFT} is deliberately NOT here.</b> #26 refuses to put a reviewer on a draft
     * because there is nothing to assess yet — but keying a paper form in and handing it to
     * somebody to chase the family for what is missing is a real day's work, and refusing it would
     * be inventing a rule the plan does not have.
     *
     * <p>Used by {@code assignOfficer()}.
     */
    private static final Set<AdmissionApplicationStatus> WORK_IS_OVER = EnumSet.of(
            AdmissionApplicationStatus.REJECTED,
            AdmissionApplicationStatus.WITHDRAWN,
            AdmissionApplicationStatus.ENROLLED);

    /**
     * The review statuses that mean <b>somebody is still assessing this application</b>.
     *
     * <p><b>{@code CANCELLED} is deliberately NOT here.</b> A cancelled review is work the school
     * called off, which is a settled answer — it is not outstanding, and waiting for it would mean
     * waiting for something that is never going to happen. {@code COMPLETED} is settled for the
     * obvious reason.
     *
     * <p>Used by {@code decide()} to refuse an approval while a review is still open.
     */
    private static final Set<AdmissionReviewStatus> STILL_ASSESSING =
            EnumSet.of(AdmissionReviewStatus.PENDING, AdmissionReviewStatus.IN_PROGRESS);

    /** The reachable statuses as a sentence, so a refusal can list them. Used by: decide(). */
    private static String names(Set<AdmissionApplicationStatus> allowed) {
        return allowed.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }

    /**
     * Endpoint #25 — one application in full.
     *
     * <p><b>Everything #24 left off</b>, plus the two things that are not on the application
     * document at all: its reviews and its offers, which live in their own collections.
     *
     * <p><b>Three collections are read, and that is the endpoint's whole cost.</b> The cycle for
     * its name, the classes for theirs, and reviews and offers for the history — four queries, all
     * of them by id or by an indexed pair, none of them per row.
     *
     * <p><b>No gate runs.</b> A read, so a suspended school can still open a form it already took.
     */
    public AdmissionApplicationDetailResponse getApplication(String admissionApplicationId) {

        //! step 1 - who is asking. `require`, not `requireUsable`: a school that cannot be edited
        //! can still read what a family sent it.
        School school = currentSchool.require();
        log.info("[getApplication] Step 1: Reading application {} for school {}",
                admissionApplicationId, school.getId());

        //! step 2 - the application, scoped by school in the QUERY. An id from another school is a
        //! real id: finding it first and checking the school afterwards would already have read a
        //! child's date of birth and their guardians' phone numbers.
        AdmissionApplication application = utils.loadApplication(school, admissionApplicationId);

        //! step 3 - the round it went into, for its name and its year.
        //!
        //! READ TOLERANTLY, not through the helper — see the note on loadCycleOrEmpty. An
        //! application whose round is gone is a broken record; this reports it by leaving the name
        //! off rather than by refusing the whole read.
        Optional<AdmissionCycle> cycle =
                utils.loadCycleOrEmpty(school, application.getAdmissionCycleDocsId());

        String cycleName = cycle.map(AdmissionCycle::getName).orElse(null);
        String academicYear = cycle.map(AdmissionCycle::getAcademicYear).orElse(null);

        //! step 4 - the reviews and the offers. Both are EMPTY today: #26 creates a review and #29
        //! creates an offer, and neither is built. The queries are still real and still scoped, so
        //! the day those endpoints write their first row this endpoint shows it unchanged.
        // TODO: read admission reviews
        List<AdmissionReview> reviews = admissionReviews
                .findBySchoolIdAndAdmissionApplicationDocsIdOrderByReviewRoundAscCreatedAtAsc(
                        school.getId(), application.getId());

        // TODO: read admission offers
        List<AdmissionOffer> offers = admissionOffers
                .findBySchoolIdAndAdmissionApplicationDocsIdOrderByRevisionNoAsc(
                        school.getId(), application.getId());

        log.info("[getApplication] Step 2: Found {} review(s) and {} offer(s)",
                reviews.size(), offers.size());

        //! step 5 - the class names, in ONE query for the applied class and every offered one.
        //! An offer can name a different class than the application - a school assesses a child
        //! and offers another grade - so both sets are collected before anything is read.
        List<String> classIds = new ArrayList<>();
        if (application.getAppliedClassDocsId() != null) {
            classIds.add(application.getAppliedClassDocsId());
        }
        offers.stream()
                .map(AdmissionOffer::getOfferedClassDocsId)
                .filter(each -> each != null && !each.isBlank())
                .forEach(classIds::add);

        //! NO YEAR MEANS NO LOOKUP. Classes are stored per academic year and the year comes from
        //! the cycle, so a missing cycle takes the class names with it. That is the honest answer
        //! rather than a second query that guesses at the year.
        // TODO: read school classes
        List<SchoolClass> classes = academicYear == null || classIds.isEmpty()
                ? List.of()
                : schoolClasses.findBySchoolIdAndAcademicYearAndIdIn(
                        school.getId(), academicYear, classIds.stream().distinct().toList());

        //! A merge function is needed even though ids are unique: toMap throws on a duplicate key
        //! rather than keeping either of them.
        Map<String, String> classNames = classes.stream().collect(Collectors.toMap(
                SchoolClass::getId, SchoolClass::getName, (first, second) -> first));

        String appliedClassName = application.getAppliedClassDocsId() == null
                ? null
                : classNames.get(application.getAppliedClassDocsId());

        //! step 6 - the reviewers' names, in ONE query for all of them.
        //!
        //! WRITTEN WHEN #26 ARRIVED, and not before. Until something could assign a reviewer this
        //! branch had nothing to resolve and no way to be tested, so it was left out on purpose
        //! and the field came back as an id. THE ASSIGNED OFFICER JOINED IT WHEN #22 ARRIVED, for
        //! exactly the same reason — and in the SAME query rather than a second one, because it is
        //! the same collection answering the same question about one more id.
        List<String> staffIds = Stream.concat(
                        reviews.stream().map(AdmissionReview::getReviewerDocsId),
                        Stream.of(application.getAssignedAdmissionOfficerDocsId()))
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        //! NOTHING TO LOOK UP IS NOT A QUERY. Most forms have no reviews and no officer.
        // TODO: read staff
        Map<String, String> staffNames = staffIds.isEmpty()
                ? Map.of()
                : staff.findBySchoolIdAndIdIn(school.getId(), staffIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                                (first, second) -> first));

        //! step 7 - the answer.
        return AdmissionApplicationDetailResponse.fromApplication(application, cycleName,
                academicYear, appliedClassName, reviews, offers, classNames, staffNames,
                utils.nextStepFor(application) + " " + NO_AUTHORIZATION_YET);
    }

}
