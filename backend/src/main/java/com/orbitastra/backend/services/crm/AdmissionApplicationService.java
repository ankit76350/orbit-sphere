package com.orbitastra.backend.services.crm;

import java.time.Instant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationCreateRequest;
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
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissioncycle.AdmissionCycleRepository;
import com.orbitastra.backend.repositories.crm.admissionoffer.AdmissionOfferRepository;
import com.orbitastra.backend.repositories.crm.admissionreview.AdmissionReviewRepository;
import com.orbitastra.backend.repositories.crm.inquiry.InquiryRepository;
import com.orbitastra.backend.services.crm.helper.CrmHelper;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;

/**
 * Admission applications — the form a family fills in. Endpoints #17, #19, #24 and #25 of the plan in
 * {@code controllers/crm/README.md}; only those four are built.
 *
 * <p><b>This is the first thing in the module that needs a cycle to be OPEN</b>, which is what #3
 * made possible. Before it, no cycle could leave DRAFT and nothing could be applied to.
 *
 * <p><b>An application does not need an inquiry.</b> The family that walks in with a completed
 * form never enquired, so {@code inquiryDocsId} is nullable — and that is why the whole pipeline
 * is testable without a single lead in the database.
 */
@Service
@RequiredArgsConstructor
public class AdmissionApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionApplicationService.class);

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

    private final AdmissionApplicationRepository applications;
    private final InquiryRepository inquiries;
    private final AdmissionCycleRepository admissionCycles;
    private final AdmissionReviewRepository admissionReviews;
    private final AdmissionOfferRepository admissionOffers;
    private final SchoolClassRepository schoolClasses;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final CrmHelper helper;

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

        //! step 4 - thin rows. The guardians, the answers and the evidence are on #25.
        return PageResponse.from(found, AdmissionApplicationSummaryResponse::fromApplication);
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
        String id = admissionApplicationId == null ? "" : admissionApplicationId.trim();
        log.info("[submitApplication] Step 1: Submitting application {} for school {}",
                id, school.getId());

        //! step 2 - the form, scoped by school in the QUERY. An id from another school is a real
        //! id, and submitting somebody else's form is worse than reading it.
        // TODO: read admission application
        AdmissionApplication application = applications.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND",
                        "No admission application with id '" + id + "' in this school."));

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
        // TODO: read school class
        String appliedClassName = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(application.getAppliedClassDocsId(),
                        school.getId(), cycle.getAcademicYear())
                .map(SchoolClass::getName)
                .orElse(null);

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
        String id = admissionApplicationId == null ? "" : admissionApplicationId.trim();
        log.info("[getApplication] Step 1: Reading application {} for school {}",
                id, school.getId());

        //! step 2 - the application, scoped by school in the QUERY. An id from another school is a
        //! real id: finding it first and checking the school afterwards would already have read a
        //! child's date of birth and their guardians' phone numbers.
        // TODO: read admission application
        AdmissionApplication application = applications.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND",
                        "No admission application with id '" + id + "' in this school."));

        //! step 3 - the round it went into, for its name and its year.
        //!
        //! READ TOLERANTLY, not through the helper. `loadCycle` throws when the cycle is missing,
        //! which is right for the four endpoints that are ABOUT a cycle — but here the caller
        //! asked for an application, and answering "no admission cycle found" to that would be a
        //! confusing 404 for a form that exists and can be read perfectly well. An application
        //! whose round is gone is a broken record; this reports it by leaving the name off rather
        //! than by refusing, the same call #6 makes about a seat row naming a deleted class.
        // TODO: read admission cycle
        Optional<AdmissionCycle> cycle = admissionCycles.findByIdAndSchoolId(
                application.getAdmissionCycleDocsId(), school.getId());

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

        //! step 6 - the answer.
        return AdmissionApplicationDetailResponse.fromApplication(application, cycleName,
                academicYear, appliedClassName, reviews, offers, classNames,
                nextStepFor(application) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * What can be done to this application next, in plain words.
     *
     * <p>Inline as a private method rather than in the helper: it is used by one endpoint, and the
     * folder rules keep single-use logic where it is used.
     *
     * <p><b>It names the endpoint AND says whether it exists.</b> Most of these are not built, and
     * an answer that said "submit it" without saying nothing can would send somebody looking for a
     * route that 404s.
     */
    private static String nextStepFor(AdmissionApplication application) {
        return switch (application.getStatus()) {
            case DRAFT -> "It is still a draft, so the family can keep editing it. #19 submits it "
                    + "and is not built, so nothing can move it on yet.";
            //! SAYS THE FORM IS FROZEN, which #19 is the moment of. It was left out until the
            //! #19 suite asked #25 what a submitted form says and got an answer that never
            //! mentioned the one thing that changed — a reader would not learn that #18 now
            //! refuses until they tried it.
            case SUBMITTED -> "It has been submitted, so the form is frozen — #18 refuses to edit "
                    + "it from here. It is waiting to be looked at: #26 assigns a reviewer and "
                    + "#20 records a decision; neither is built.";
            case UNDER_REVIEW -> "Somebody is reviewing it. #27 records the result and is not "
                    + "built.";
            case ADDITIONAL_INFORMATION_REQUIRED -> "The school asked the family for something "
                    + "more. It moves on once that arrives.";
            case WAITLISTED -> "It was neither approved nor rejected — the school is holding it "
                    + "for a seat. An offer can still be made from here.";
            case APPROVED -> "It has been approved, so an offer can be issued. #29 issues one and "
                    + "is not built.";
            case REJECTED -> "The school decided against it. Nothing moves from here.";
            case WITHDRAWN -> "The family pulled out. Nothing moves from here.";
            case OFFERED -> "An offer is out with the family. #30 records their answer and is not "
                    + "built.";
            case OFFER_ACCEPTED -> "The family accepted. #33 turns the applicant into a student "
                    + "and is not built — this is where the module runs out of road.";
            case ENROLLED -> "The child is a student now, and this application is history.";
        };
    }
}
