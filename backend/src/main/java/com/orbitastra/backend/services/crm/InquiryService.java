package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryCreateRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryFollowUpRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquirySearchRequest;
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryUpdateRequest;
import com.orbitastra.backend.dto.crm.inquiry.response.InquiryDetailResponse;
import com.orbitastra.backend.dto.crm.inquiry.response.InquiryResponse;
import com.orbitastra.backend.dto.crm.inquiry.response.InquirySummaryResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.crm.inquiry.InquiryRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.crm.utils.InquiryServiceUtils;
import com.orbitastra.backend.services.institution.NumberSequenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The lead half of admissions. Endpoints #8, #13 and #14 of the plan in this package's README; the
 * rest of #9 to #16 are not built.
 *
 * <p><b>The module's other four collections were built first, and that was deliberate.</b> An
 * application does <i>not</i> need an inquiry — {@code inquiryDocsId} is nullable, for the family
 * that walks in with a completed form — so the whole pipeline is testable end to end without a
 * single lead in the database. Leads were the one block nothing else depended on.
 *
 * <p><b>Which left two write paths nothing could reach.</b> #17 moves a named lead to
 * {@code APPLICATION_STARTED} and #19 to {@code APPLICATION_SUBMITTED}, and until #8 existed there
 * was no way to create an inquiry through the API at all — those branches were only reachable by
 * writing to Mongo directly, which is how the suites have been exercising them.
 *
 * <p><b>It had no {@code utils} file for its first three endpoints</b>, and that was right: #8
 * writes, #13 pages and #14 reads one, and nothing in that set repeated. <b>#9 is what earned
 * one</b> — correcting a lead starts exactly where opening one does — and the folder rule is that
 * a read moves there at <i>two</i> callers, not in anticipation of them.
 *
 * <p><b>What stayed private, and why.</b> {@code overdueNow}, {@code contactNumberOf} and
 * {@code nextStepFor} have two or more callers each and are still here, because the rule counts
 * callers for <i>reads</i> — these touch no repository at all. They are sentences about a document
 * already in hand, and a {@code utils} full of those is a longer import list and a jump to nowhere.
 *
 * <p><b>The one thing #13 and #14 share that mattered most is the {@code overdue} rule</b>, and
 * that is exactly why it is one method rather than two: a worklist row and the lead it opens
 * disagreeing about whether a family is owed a call is the bug this avoids.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "NOTE: nothing checks who is asking yet.";

    /**
     * The statuses a lead is finished in — the ones {@code overdue} ignores.
     *
     * <p>Mirrors {@code FINISHED} in the repository, which is where the <i>query</i> uses it. This
     * copy is for working the flag out on a row that has already been read: #13 reports
     * {@code overdue} on every row, not only the ones a filter asked for.
     *
     * <p>Used by {@code overdueNow()}.
     */
    private static final Set<InquiryStatus> FINISHED =
            EnumSet.of(InquiryStatus.LOST, InquiryStatus.CLOSED);

    /**
     * <b>Where a lead may go next. This table is the product rule</b>, the same way
     * {@code CYCLE_MOVES}, {@code DECISION_MOVES} and {@code REVIEW_MOVES} are in this module.
     *
     * <p>It is the graph in this package's README, written out. <b>Both terminal statuses are
     * spelled out with an empty set</b> rather than left off the map: "nothing follows LOST" is a
     * decision, and a missing key would be a gap that reads the same as a forgotten one.
     *
     * <p><b>{@code LOST} is reachable from every non-terminal status</b>, which is what makes it
     * worth writing this out — a family can stop answering at any point.
     *
     * <p><b>What this table permits is not all #10 permits.</b> Three destinations are refused on
     * top of it, because another endpoint owns them: {@code LOST} needs a reason (#12), and
     * {@code APPLICATION_STARTED} and {@code APPLICATION_SUBMITTED} are facts about an application
     * (#17 and #19). They are in the table because they are legal <i>moves</i>; who may make them
     * is a separate question, and keeping the two apart is what stops the table lying about the
     * product when #12 arrives.
     *
     * <p>Used by {@code allowedNext()}.
     */
    private static final Map<InquiryStatus, Set<InquiryStatus>> LEAD_MOVES = Map.of(
            InquiryStatus.NEW, EnumSet.of(InquiryStatus.CONTACTED, InquiryStatus.LOST),
            InquiryStatus.CONTACTED, EnumSet.of(InquiryStatus.COUNSELLING,
                    InquiryStatus.APPLICATION_STARTED, InquiryStatus.LOST),
            InquiryStatus.COUNSELLING, EnumSet.of(InquiryStatus.VISIT_SCHEDULED,
                    InquiryStatus.APPLICATION_STARTED, InquiryStatus.LOST),
            InquiryStatus.VISIT_SCHEDULED, EnumSet.of(InquiryStatus.VISITED, InquiryStatus.LOST),
            InquiryStatus.VISITED, EnumSet.of(InquiryStatus.APPLICATION_STARTED,
                    InquiryStatus.LOST),
            InquiryStatus.APPLICATION_STARTED, EnumSet.of(InquiryStatus.APPLICATION_SUBMITTED,
                    InquiryStatus.LOST),
            InquiryStatus.APPLICATION_SUBMITTED, EnumSet.of(InquiryStatus.CLOSED),
            InquiryStatus.LOST, EnumSet.noneOf(InquiryStatus.class),
            InquiryStatus.CLOSED, EnumSet.noneOf(InquiryStatus.class));

    /**
     * The two statuses no endpoint may be <i>told</i> to set, whatever the table says.
     *
     * <p><b>A lead's application state is a fact about the application.</b> #17 sets
     * {@code APPLICATION_STARTED} as a side effect of a form being started and #19 sets
     * {@code APPLICATION_SUBMITTED} when it is sent. Letting a counsellor type either would let
     * the lead claim a form that does not exist — and the lead is the half nobody checks.
     *
     * <p>Used by {@code logFollowUp()}.
     */
    private static final Set<InquiryStatus> NOT_BY_HAND = EnumSet.of(
            InquiryStatus.APPLICATION_STARTED, InquiryStatus.APPLICATION_SUBMITTED);

    /**
     * The fields #13 may be ordered by: what a caller types -> the field on the document.
     *
     * <p><b>An allowlist is a security control, not a convenience</b> — ordering is a read, and
     * sorting by a field walks its values out of the database a page at a time.
     *
     * <p>{@code notes}, {@code lostReason}, {@code sourceDetails} and {@code guardians} are
     * deliberately absent. Three of them are free text a counsellor wrote about a family, and the
     * fourth would sort by its first element, which means nothing.
     */
    private static final Map<String, String> SORTABLE_INQUIRY_FIELDS = new LinkedHashMap<>();

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_INQUIRY_FIELD_NAMES;

    /**
     * The default order: soonest to chase first, then by id.
     *
     * <p><b>A worklist is sorted by when the next call is due</b>, which is the whole of what #13
     * is for — and it is the last key of {@code school_inquiry_pipeline_idx}.
     *
     * <p><b>{@code id} is the tiebreaker, and it has to be something.</b> A lead's unique business
     * key is its number, but that is only unique per school; the document id is the one total order
     * available. Without it, two leads sharing a date swap places between pages and one row is
     * shown twice while another is never shown.
     *
     * <p><b>A lead with no follow-up date sorts FIRST</b>, because Mongo puts a missing field
     * before every value. On a chase list that is the wrong end — but it is also the honest one:
     * a lead nobody has promised to ring is the one most likely to be forgotten.
     */
    private static final Sort INQUIRY_ORDER =
            Sort.by(Sort.Order.asc("nextFollowUpAt"), Sort.Order.asc("id"));

    static {
        SORTABLE_INQUIRY_FIELDS.put("nextfollowupat", "nextFollowUpAt");
        SORTABLE_INQUIRY_FIELDS.put("prospectivestudentname", "prospectiveStudentName");
        SORTABLE_INQUIRY_FIELDS.put("inquiryno", "inquiryNo");
        SORTABLE_INQUIRY_FIELDS.put("status", "status");
        SORTABLE_INQUIRY_FIELDS.put("academicyear", "academicYear");
        SORTABLE_INQUIRY_FIELDS.put("createdat", "createdAt");
        SORTABLE_INQUIRY_FIELDS.put("updatedat", "updatedAt");
        SORTABLE_INQUIRY_FIELD_NAMES = String.join(", ", SORTABLE_INQUIRY_FIELDS.values());
    }

    private final InquiryRepository inquiries;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;
    private final NumberSequenceService numberSequences;
    private final CurrentSchoolResolver currentSchool;
    private final InquiryServiceUtils utils;

    /**
     * Endpoint #8 — the front desk captures a lead.
     *
     * <p><b>Almost everything is optional, and that is the point.</b> A phone call is "a mother
     * rang about her son for next year" — a name, a year, and nothing else. An endpoint that
     * demanded a date of birth and a guardian's email would refuse the commonest lead there is,
     * and the front desk would stop using it.
     *
     * <p><b>It creates {@code NEW} and nothing else.</b> Every other status is somebody having
     * done something — #10 logs a call, #12 moves it, #17 and #19 move it as a side effect of the
     * family applying. A lead captured is a lead nobody has acted on yet.
     *
     * <p><b>It does NOT check for duplicates.</b> #15 is what asks "is this family already known",
     * and it is asked <i>before</i> this — by the person at the desk, who can see the answer and
     * decide. Refusing here would be this endpoint guessing that two children with one phone
     * number are the same enquiry, when a family with two children is exactly that.
     */
    public InquiryResponse createInquiry(InquiryCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[createInquiry] Step 1: Capturing a lead for school {}", school.getId());

        //! step 2 - the year has to exist. NOT the running one: a lead is about an intake that has
        //! not started, which is the normal case rather than the edge — the same reading #1 makes
        //! about a cycle.
        String year = request.academicYear().trim();

        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! step 3 - the class they are interested in, when they named one. OPTIONAL: most phone
        //! calls do not. It has to be a class of THAT year — a lead about a class the year does
        //! not have is a lead nobody can act on.
        String classId = TextHelper.blankToNull(request.interestedClassDocsId());
        SchoolClass interested = null;

        if (classId != null) {
            // TODO: read school class
            interested = schoolClasses
                    .findByIdAndSchoolIdAndAcademicYear(classId, school.getId(), year)
                    .orElseThrow(() -> ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                            "Class '" + classId + "' is not a class of '" + year + "', which is "
                                    + "the year this lead is about."));
        }

        //! step 4 - the counsellor, when the desk hands it straight to somebody. OPTIONAL because
        //! most leads are captured first and assigned after — #11 is what assigns one later.
        String counselorId = TextHelper.blankToNull(request.assignedCounselorDocsId());
        Staff counselor = null;

        if (counselorId != null) {
            // TODO: read staff
            counselor = staff.findByIdAndSchoolId(counselorId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + counselorId + "' in this school, so the "
                                    + "lead cannot be given to them."));
        }

        //! step 5 - the number. Generated, never supplied: nobody picks their own inquiry number.
        String inquiryNo = numberSequences.next(school.getId(),
                NumberSequenceType.ADMISSION_INQUIRY, "INQ/{YYYY}/{MM}/");
        log.info("[createInquiry] Step 2: Allocated inquiry number {}", inquiryNo);

        //! step 6 - build it. schoolId set by hand: nothing fills it in, and a row without it
        //! belongs to no school and is invisible to every read.
        //!
        //! GUARDIANS ARE OPTIONAL AND SO IS EVERY FIELD ON ONE. A walk-in who gives a child's name
        //! and a phone number is a real lead; a record that insisted on a relation would refuse it.
        //! #15 finds a family again by phone or email, and it can only find the ones who left one.
        Inquiry inquiry = Inquiry.builder()
                .schoolId(school.getId())
                .inquiryNo(inquiryNo)
                .prospectiveStudentName(request.prospectiveStudentName().trim())
                .academicYear(year)
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .interestedClassDocsId(interested == null ? null : interested.getId())
                .status(InquiryStatus.NEW)
                .assignedCounselorDocsId(counselor == null ? null : counselor.getId())
                .source(TextHelper.blankToNull(request.source()))
                .sourceDetails(TextHelper.blankToNull(request.sourceDetails()))
                .notes(TextHelper.blankToNull(request.notes()))
                .guardians(request.guardians() == null ? new ArrayList<>()
                        : request.guardians().stream()
                                .map(one -> InquiryGuardian.builder()
                                        .fullName(TextHelper.blankToNull(one.fullName()))
                                        .relation(one.relation())
                                        .phoneNumber(TextHelper.blankToNull(one.phoneNumber()))
                                        .emailAddress(TextHelper.blankToNull(one.emailAddress()))
                                        .address(TextHelper.blankToNull(one.address()))
                                        .occupation(TextHelper.blankToNull(one.occupation()))
                                        .primaryContact(one.primaryContact() != null
                                                && one.primaryContact())
                                        .build())
                                .collect(Collectors.toCollection(ArrayList::new)))
                .followUps(new ArrayList<>())
                .build();

        //! step 7 - save
        // TODO: insert inquiry
        Inquiry saved = inquiries.save(inquiry);
        log.info("[createInquiry] Step 3: Captured lead {} as {}", saved.getId(), inquiryNo);

        return InquiryResponse.fromInquiry(saved,
                interested == null ? null : interested.getName(),
                counselor == null ? null : counselor.getFullName(),
                nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #9 — <b>correct what the front desk wrote down</b>.
     *
     * <p><b>A lead is the school's own notes, not a declaration the family signed</b> — which is
     * why, unlike #18, <b>there is no status gate</b>. #18 refuses anything but {@code DRAFT}
     * because #19 freezes a snapshot of what the family declared, and a school that could rewrite
     * that afterwards could not answer what they actually said. Nobody declares a lead. Somebody
     * took a phone call and wrote down what they heard, and the commonest thing that happens to a
     * phone call is mishearing it — so a {@code LOST} lead can still have a misspelt name put
     * right, and correcting one that reached {@code APPLICATION_SUBMITTED} touches no application:
     * #17 <b>copies</b> the guardians onto the form at the start, so the two have been separate
     * records ever since.
     *
     * <p><b>A blank string clears an optional field; an absent one leaves it alone.</b> That is
     * how a caller says "they no longer have a class in mind". A <b>required</b> field refuses a
     * blank instead, exactly as #18 refuses an empty {@code applicantName}.
     *
     * <p><b>Moving the year re-checks the class.</b> A lead's interested class must be a class of
     * the year the lead is about — #8 enforces it and #14 resolves the name with it — so a year
     * that moves and leaves an unrelated class behind would break the one invariant these two
     * fields have. The refusal names the way out: send a class of the new year, or clear it.
     *
     * <p><b>Two gates.</b> A write.
     */
    public InquiryResponse updateInquiry(String inquiryId, InquiryUpdateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[updateInquiry] Step 1: Correcting lead {} for school {}", inquiryId,
                school.getId());

        //! step 2 - the lead, scoped by school in the QUERY.
        Inquiry inquiry = utils.loadInquiry(school, inquiryId);

        //! step 3 - is the body carrying anything at all.
        //!
        //! FIRST, BEFORE THE VERSION — the order #18, #27 and #29b settled on. It is the only
        //! check about the REQUEST rather than about the world, and a body that asks for nothing
        //! is meaningless whatever state the lead is in.
        boolean movesSomething = request.prospectiveStudentName() != null
                || request.academicYear() != null
                || request.dateOfBirth() != null
                || request.gender() != null
                || request.interestedClassDocsId() != null
                || request.guardians() != null
                || request.source() != null
                || request.sourceDetails() != null
                || request.notes() != null;

        if (!movesSomething) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "This request changes nothing. Send a prospectiveStudentName, an "
                            + "academicYear, a dateOfBirth, a gender, an interestedClassDocsId, "
                            + "guardians, a source, sourceDetails or notes.");
        }

        //! step 4 - somebody else may have moved it while this caller was reading.
        if (request.version() != null && !request.version().equals(inquiry.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + inquiry.getProspectiveStudentName() + "' changed since you read it. "
                            + "Read it again before correcting it.");
        }

        //! NOTE: NO STATUS CHECK, and its absence is the design. See this method's doc.

        //! step 5 - the year, when it is being corrected. It has to exist, and NOT be the running
        //! one: a lead is about an intake that has not started, which is the normal case.
        String year = inquiry.getAcademicYear();

        if (request.academicYear() != null) {
            year = request.academicYear().trim();
            if (year.isEmpty()) {
                throw ApiException.badRequest("BLANK_ACADEMIC_YEAR",
                        "A lead is always about an intake. Send a year, or leave the field out to "
                                + "keep the one it has.");
            }

            // TODO: check academic year exists
            if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
                throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + year + "' in this school.");
            }
        }

        //! step 6 - the class. THREE CASES, and the third is the one worth having:
        //!
        //!   a) a class was sent -> check it against `year`, which is the NEW year when the year
        //!      moved in step 5 and the stored one otherwise;
        //!   b) "" was sent      -> clear it. The family no longer has a class in mind;
        //!   c) nothing was sent BUT THE YEAR MOVED -> re-check the class already on the lead.
        //!
        //! (c) IS THE INVARIANT. #8 enforces "the interested class is a class of the lead's year"
        //! and #14 resolves the name with it; a year that moved and left an unrelated class behind
        //! would break it silently, and the lead would come back with no class name and no reason.
        boolean clearClass = false;
        SchoolClass interested = null;
        String wantedClass = request.interestedClassDocsId();

        if (wantedClass != null && wantedClass.isBlank()) {
            clearClass = true;
        } else if (wantedClass != null) {
            interested = requireClassOfYear(school, wantedClass.trim(), year, false);
        } else if (request.academicYear() != null
                && inquiry.getInterestedClassDocsId() != null) {
            interested = requireClassOfYear(school, inquiry.getInterestedClassDocsId(), year, true);
        }

        //! step 7 - build the change. ONLY WHAT WAS SENT, so correcting a name does not clear the
        //! notes somebody typed yesterday.
        if (request.prospectiveStudentName() != null) {
            //! NOT blankToNull. A name is required on this document, so "" is a caller trying to
            //! remove one — and @NotBlank is not on the DTO field because every field is optional.
            //! Refusing is the honest answer rather than storing an empty name.
            String name = request.prospectiveStudentName().trim();
            if (name.isEmpty()) {
                throw ApiException.badRequest("BLANK_STUDENT_NAME",
                        "A lead has to be about somebody. Send a name, or leave the field out to "
                                + "keep the one it has.");
            }
            inquiry.setProspectiveStudentName(name);
        }
        if (request.academicYear() != null) {
            inquiry.setAcademicYear(year);
        }
        if (request.dateOfBirth() != null) {
            inquiry.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null) {
            inquiry.setGender(request.gender());
        }
        if (clearClass) {
            inquiry.setInterestedClassDocsId(null);
        } else if (interested != null) {
            inquiry.setInterestedClassDocsId(interested.getId());
        }
        //! REPLACED WHOLE, so `[]` clears them. A guardian has no id to merge by — they are
        //! embedded, not documents — and merging would leave no way to remove one added by
        //! mistake.
        //!
        //! AN EMPTY LIST IS ALLOWED HERE AND REFUSED BY #18, and the difference is real: an
        //! application with no guardian is not one a school can act on, but a LEAD with none is
        //! the walk-in who gave a child's name and left, which #8 is built to accept.
        if (request.guardians() != null) {
            inquiry.setGuardians(request.guardians().stream()
                    .map(one -> InquiryGuardian.builder()
                            .fullName(TextHelper.blankToNull(one.fullName()))
                            .relation(one.relation())
                            .phoneNumber(TextHelper.blankToNull(one.phoneNumber()))
                            .emailAddress(TextHelper.blankToNull(one.emailAddress()))
                            .address(TextHelper.blankToNull(one.address()))
                            .occupation(TextHelper.blankToNull(one.occupation()))
                            .primaryContact(one.primaryContact() != null && one.primaryContact())
                            .build())
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        //! THE THREE FREE-TEXT FIELDS, where "" means "that was a mistake, take it off". None is
        //! required, so blankToNull is exactly right: it stores null for a blank rather than an
        //! empty string, and a field that is absent never reaches here at all.
        if (request.source() != null) {
            inquiry.setSource(TextHelper.blankToNull(request.source()));
        }
        if (request.sourceDetails() != null) {
            inquiry.setSourceDetails(TextHelper.blankToNull(request.sourceDetails()));
        }
        if (request.notes() != null) {
            inquiry.setNotes(TextHelper.blankToNull(request.notes()));
        }

        //! step 8 - save
        // TODO: update inquiry
        Inquiry saved = inquiries.save(inquiry);
        log.info("[updateInquiry] Step 2: Lead {} corrected", saved.getId());

        //! step 9 - the names, for the answer. TOLERANTLY, as everywhere here: a counsellor who
        //! has left is reported by leaving the name off rather than by refusing the correction.
        String className = null;
        if (saved.getInterestedClassDocsId() != null) {
            // TODO: read school class
            className = schoolClasses
                    .findByIdAndSchoolIdAndAcademicYear(saved.getInterestedClassDocsId(),
                            school.getId(), saved.getAcademicYear())
                    .map(SchoolClass::getName)
                    .orElse(null);
        }

        String counselorName = null;
        if (saved.getAssignedCounselorDocsId() != null) {
            // TODO: read staff
            counselorName = staff
                    .findByIdAndSchoolId(saved.getAssignedCounselorDocsId(), school.getId())
                    .map(Staff::getFullName)
                    .orElse(null);
        }

        return InquiryResponse.fromInquiry(saved, className, counselorName,
                nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * The class a lead names, checked against the year the lead is about.
     *
     * <p><b>Two callers, and the second is the interesting one</b> — #9 asks it both about a class
     * the caller just sent and about the one already on the document, when the <i>year</i> moved
     * underneath it. {@code alreadyStored} is what makes the refusal say which of those happened,
     * because "that class is not of that year" is useless advice when the caller never mentioned a
     * class.
     *
     * <p><b>Private and inline rather than in {@code utils}</b>: one endpoint calls it. #8 asks
     * the same question and does not use this, because #8 asks it unconditionally and in one
     * place, where it reads in the order it happens.
     *
     * Used by: updateInquiry().
     */
    private SchoolClass requireClassOfYear(School school, String classDocsId, String year,
            boolean alreadyStored) {

        // TODO: read school class
        return schoolClasses.findByIdAndSchoolIdAndAcademicYear(classDocsId, school.getId(), year)
                .orElseThrow(() -> ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                        alreadyStored
                                ? "This lead is interested in class '" + classDocsId + "', which "
                                        + "is not a class of '" + year + "'. Send an "
                                        + "interestedClassDocsId of that year as well, or send "
                                        + "\"\" to clear it."
                                : "Class '" + classDocsId + "' is not a class of '" + year
                                        + "', which is the year this lead is about."));
    }

    /**
     * Endpoint #10 — <b>log one interaction</b>.
     *
     * <p><b>This is the endpoint the lead half was waiting for.</b> #13 sorts a worklist by
     * {@code nextFollowUpAt} and #14 renders a timeline, and until this existed every lead in the
     * database had an empty timeline and no chase date — both reads were correct and had nothing
     * to show. This writes the only two fields either of them is really about.
     *
     * <p><b>A {@code $push}, never a re-save.</b> Reading the lead, adding to its list and saving
     * the whole document back would overwrite every entry anybody else logged in between, and a
     * timeline is exactly the kind of list two counsellors write to at once.
     *
     * <p><b>The chase date is rewritten every time, including to nothing.</b> The field means "the
     * next call is due at"; once this call has been made and no new date promised, there is no next
     * call due. Leaving the old one would keep showing a family as overdue on the day somebody rang
     * them. <b>The entry keeps what was promised</b>, so the history is not lost.
     *
     * <p><b>Moving the status is optional and walks the table.</b> Most calls move nothing — a
     * counsellor rings, nobody answers — and the entry stores exactly what was sent, so a null on
     * the timeline reads "left as it was".
     *
     * <p><b>No status gate on the lead itself.</b> A {@code LOST} lead cannot be moved anywhere
     * (the table says so) but a note can still be logged against it: somebody ringing back a family
     * that gave up is exactly the call worth recording.
     *
     * <p><b>Two gates.</b> A write.
     */
    public InquiryDetailResponse logFollowUp(String inquiryId, InquiryFollowUpRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[logFollowUp] Step 1: Logging a follow-up on lead {} for school {}", inquiryId,
                school.getId());

        //! step 2 - the lead, scoped by school in the QUERY.
        Inquiry inquiry = utils.loadInquiry(school, inquiryId);

        //! step 3 - somebody else may have moved it while this caller was reading. CHECKED HERE
        //! AND GUARDED IN THE QUERY: this one gives the caller a sentence, that one wins the race.
        if (request.version() != null && !request.version().equals(inquiry.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + inquiry.getProspectiveStudentName() + "' changed since you read it. "
                            + "Read it again before logging against it.");
        }

        //! step 4 - who logged it, when the caller says. OPTIONAL, and it should not be: nothing
        //! in this project knows who is asking yet, so the only way to fill it is to be told. #14
        //! renders the gap as "not recorded" rather than hiding the entry.
        String counselorId = TextHelper.blankToNull(request.counselorDocsId());

        if (counselorId != null) {
            // TODO: read staff
            staff.findByIdAndSchoolId(counselorId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + counselorId + "' in this school, so the "
                                    + "follow-up cannot be recorded against them."));
        }

        //! step 5 - the move, when the call made one.
        InquiryStatus moved = request.status();

        if (moved != null && moved != inquiry.getStatus()) {

            //! FIRST, THE TWO NOBODY MAY TYPE. Checked BEFORE the table, because the table
            //! permits them: they are legal moves owned by another endpoint, and a caller who
            //! sent APPLICATION_STARTED deserves to be told who does set it rather than that the
            //! move is impossible — which would be a lie.
            if (NOT_BY_HAND.contains(moved)) {
                throw ApiException.conflict("INQUIRY_STATUS_NOT_BY_HAND",
                        moved + " is a fact about an application, not something a follow-up may "
                                + "claim. #17 sets it when a form is started and #19 when it is "
                                + "submitted — otherwise a lead could claim a form that does not "
                                + "exist.");
            }

            //! THEN LOST, which is legal from everywhere and needs a reason this endpoint has
            //! nowhere to put.
            if (moved == InquiryStatus.LOST) {
                throw ApiException.conflict("LOST_NEEDS_A_REASON",
                        "Marking a lead LOST needs a reason, and a follow-up has nowhere to put "
                                + "one. #12 is what gives up on a lead. Log what happened here "
                                + "and lose it there.");
            }

            //! THEN THE TABLE, which is the product rule.
            Set<InquiryStatus> allowed = allowedNext(inquiry.getStatus());

            if (!allowed.contains(moved)) {
                throw ApiException.conflict("INQUIRY_TRANSITION_NOT_ALLOWED",
                        "'" + inquiry.getProspectiveStudentName() + "' is " + inquiry.getStatus()
                                + " and cannot go to " + moved + ". It can go to: "
                                + names(allowed) + ".");
            }
        }

        //! step 6 - build the entry. recordedAt IS THE SERVER'S: a caller who could name the time
        //! a call happened could log one into next week, and a timeline sorted on a
        //! caller-supplied instant is not a record of anything.
        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(request.status())
                .note(request.note().trim())
                .communicationChannel(TextHelper.blankToNull(request.communicationChannel()))
                .nextFollowUpAt(request.nextFollowUpAt())
                .counselorDocsId(counselorId)
                .recordedAt(Instant.now())
                .build();

        //! step 7 - one atomic update: the entry pushed, the chase date rewritten, the status
        //! moved when it moved. NOT A SAVE — see the repository.
        // TODO: update inquiry (append one follow-up)
        long moveCount = inquiries.pushFollowUp(school.getId(), inquiry.getId(), entry,
                request.nextFollowUpAt(),
                moved != null && moved != inquiry.getStatus() ? moved : null,
                request.version());

        //! NOTHING MOVED means the lead went, or somebody won the race. Re-reading is what tells
        //! those two apart, and it is worth the extra query: "it is gone" and "you were too slow"
        //! are different things to be told.
        if (moveCount == 0) {
            Inquiry now = inquiries.findByIdAndSchoolId(inquiry.getId(), school.getId())
                    .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND",
                            "No inquiry with id '" + inquiry.getId() + "' in this school."));

            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + now.getProspectiveStudentName() + "' changed while this was being "
                            + "written. Read it again before logging against it.");
        }
        log.info("[logFollowUp] Step 2: Logged a follow-up on lead {}", inquiry.getId());

        //! step 8 - read it back, so the caller sees the timeline they just added to. THE WHOLE
        //! DOCUMENT, not the one built above: the push is what decided the order and the version.
        // TODO: read inquiry
        Inquiry saved = utils.loadInquiry(school, inquiry.getId());

        //! step 9 - the names. THE SAME SHAPE #14 USES, and deliberately not shared with it: that
        //! one reads a lead and this one has just written to it, and a method that did both would
        //! be a method with a flag deciding which.
        String interestedClassName = null;
        if (saved.getInterestedClassDocsId() != null) {
            // TODO: read school class
            interestedClassName = schoolClasses
                    .findByIdAndSchoolIdAndAcademicYear(saved.getInterestedClassDocsId(),
                            school.getId(), saved.getAcademicYear())
                    .map(SchoolClass::getName)
                    .orElse(null);
        }

        List<String> staffIds = Stream.concat(
                        Stream.of(saved.getAssignedCounselorDocsId()),
                        (saved.getFollowUps() == null ? List.<InquiryFollowUp>of()
                                : saved.getFollowUps()).stream()
                                .map(InquiryFollowUp::getCounselorDocsId))
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        // TODO: read staff
        Map<String, String> staffNames = staffIds.isEmpty()
                ? Map.of()
                : staff.findBySchoolIdAndIdIn(school.getId(), staffIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                                (first, second) -> first));

        return InquiryDetailResponse.fromInquiry(saved, interestedClassName, staffNames,
                overdueNow(saved), nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Where this lead may go next.
     *
     * <p><b>An unknown status is nowhere, not everywhere.</b> Every value of the enum is a key in
     * the table, so this cannot happen today — and if the enum grows a value and the table does
     * not, refusing every move is the failure that gets noticed rather than the one that lets
     * anything through.
     *
     * Used by: logFollowUp().
     */
    private static Set<InquiryStatus> allowedNext(InquiryStatus from) {
        return LEAD_MOVES.getOrDefault(from, Set.of());
    }

    /**
     * The reachable statuses as a sentence, so a refusal can list them.
     *
     * <p><b>"nothing" rather than an empty string</b> for a terminal lead. A refusal that trails
     * off with "it can go to: ." reads like a bug in the message; saying <i>nothing</i> is the
     * actual answer. The same call {@code AdmissionReviewServiceUtils.names} makes, and not shared
     * with it: that one is about reviews, and one sentence-builder over two unrelated enums would
     * be a generic helper nobody can read in place.
     *
     * <p><b>Sorted</b>, so the same set always reads the same way. {@code EnumSet} iterates in
     * declaration order, which would make the sentence depend on how the enum happens to be
     * written.
     *
     * Used by: logFollowUp().
     */
    private static String names(Set<InquiryStatus> allowed) {
        return allowed.isEmpty() ? "nothing"
                : allowed.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }

    /**
     * Endpoint #13 — <b>the counsellor's worklist</b>.
     *
     * <p><b>Soonest to chase first</b>, which is the whole of what a worklist is. Filter by
     * {@code status} and {@code assignedCounselorDocsId} and you have one person's open leads;
     * those two plus {@code nextFollowUpAt} are exactly {@code school_inquiry_pipeline_idx}, which
     * exists for this.
     *
     * <p><b>{@code overdue=true} is the sharper question</b> — past its date <i>and</i> not
     * finished, because a lead somebody closed last month has a past date too.
     *
     * <p><b>A row is thinner than the lead</b>: no notes, no source details, no timeline. All
     * three can be long and a page of twenty would carry every word a counsellor ever wrote to
     * draw a list that shows none of them. <b>But the phone number is on it</b>, because the point
     * of a worklist is to pick up the phone.
     *
     * <p><b>There is no "me".</b> Nothing in this project knows who is calling yet, so whose
     * worklist it is has to be said out loud.
     *
     * <p><b>No gates.</b> A read — a suspended school still owes these calls.
     */
    public PageResponse<InquirySummaryResponse> listInquiries(InquirySearchRequest request) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();

        //! step 2 - the page, the sort and the allowlist
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_INQUIRY_FIELDS, SORTABLE_INQUIRY_FIELD_NAMES, INQUIRY_ORDER);

        // TODO: read inquiries
        Page<Inquiry> found = inquiries.search(school.getId(), request, pageable);
        log.info("[listInquiries] Step 1: Found {} lead(s) in total", found.getTotalElements());

        //! step 3 - the counsellors' names, ONE QUERY FOR THE WHOLE PAGE rather than one per row.
        //! A worklist of raw ids is not a worklist anybody can work from.
        List<String> counselorIds = found.getContent().stream()
                .map(Inquiry::getAssignedCounselorDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        //! NOTHING TO LOOK UP IS NOT A QUERY. A page of unassigned leads is the common case —
        //! #11 assigns one and is not built, so today it is the ONLY case.
        // TODO: read staff
        Map<String, String> counselorNames = counselorIds.isEmpty()
                ? Map.of()
                : staff.findBySchoolIdAndIdIn(school.getId(), counselorIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                                (first, second) -> first));

        //! step 4 - thin rows.
        return PageResponse.from(found, one -> InquirySummaryResponse.fromInquiry(one,
                one.getAssignedCounselorDocsId() == null ? null
                        : counselorNames.get(one.getAssignedCounselorDocsId()),
                contactNumberOf(one),
                overdueNow(one)));
    }

    /**
     * Endpoint #14 — <b>one lead with its whole timeline</b>.
     *
     * <p><b>Everything #13's row leaves off</b>, plus the follow-ups in the order they happened,
     * with whoever logged each one named.
     *
     * <p><b>One staff query for the whole lead</b>, not one per entry: the counsellor it is
     * assigned to and everybody who logged a follow-up are asked about together. A timeline of ten
     * calls by three people is one read.
     *
     * <p><b>Names are resolved tolerantly.</b> Somebody who has left the school still made the call
     * they made; dropping the entry or inventing a name would hide that.
     *
     * <p><b>No gates.</b> A read.
     */
    public InquiryDetailResponse getInquiry(String inquiryId) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String id = inquiryId == null ? "" : inquiryId.trim();
        log.info("[getInquiry] Step 1: Reading lead {} for school {}", id, school.getId());

        //! step 2 - the lead, scoped by school in the QUERY. An id from another school is a real
        //! id, and reading it would hand over another tenant's family details.
        Inquiry inquiry = utils.loadInquiry(school, id);

        //! step 3 - the class, when the family named one. TOLERANTLY: a class that was removed
        //! must not stop a lead being read, and leaving the name off is the honest answer.
        String interestedClassName = null;
        if (inquiry.getInterestedClassDocsId() != null) {
            // TODO: read school class
            interestedClassName = schoolClasses
                    .findByIdAndSchoolIdAndAcademicYear(inquiry.getInterestedClassDocsId(),
                            school.getId(), inquiry.getAcademicYear())
                    .map(SchoolClass::getName)
                    .orElse(null);
        }

        //! step 4 - every staff id on this lead, in ONE query. The counsellor it is assigned to
        //! and everybody who logged a follow-up: a timeline of ten calls by three people is one
        //! read, not ten.
        List<String> staffIds = Stream.concat(
                        Stream.of(inquiry.getAssignedCounselorDocsId()),
                        (inquiry.getFollowUps() == null ? List.<InquiryFollowUp>of()
                                : inquiry.getFollowUps()).stream()
                                .map(InquiryFollowUp::getCounselorDocsId))
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        // TODO: read staff
        Map<String, String> staffNames = staffIds.isEmpty()
                ? Map.of()
                : staff.findBySchoolIdAndIdIn(school.getId(), staffIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                                (first, second) -> first));

        return InquiryDetailResponse.fromInquiry(inquiry, interestedClassName, staffNames,
                overdueNow(inquiry), nextStepFor(inquiry) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Is this lead past its follow-up date and still worth chasing.
     *
     * <p><b>The same two conditions the query uses</b>, asked of a row already read. #13 reports
     * the flag on <i>every</i> row, not only the ones a filter asked for — a caller listing
     * everything still wants to see which are late, and making them compare a timestamp themselves
     * is how two screens end up disagreeing about what "overdue" means.
     *
     * Used by:
     * - listInquiries()
     * - getInquiry()
     */
    private static boolean overdueNow(Inquiry inquiry) {
        return inquiry.getNextFollowUpAt() != null
                && inquiry.getNextFollowUpAt().isBefore(Instant.now())
                && !FINISHED.contains(inquiry.getStatus());
    }

    /**
     * The number to ring, for a worklist row.
     *
     * <p><b>The primary guardian's, or the first one with a number.</b> A row that showed nothing
     * because the first guardian happened to have no phone would be a row nobody can use — and a
     * lead may carry a guardian with a number and no name at all, which is exactly what #8 is
     * built to accept.
     *
     * Used by: listInquiries().
     */
    private static String contactNumberOf(Inquiry inquiry) {
        if (inquiry.getGuardians() == null) {
            return null;
        }

        return inquiry.getGuardians().stream()
                .filter(one -> one.getPhoneNumber() != null && !one.getPhoneNumber().isBlank())
                .sorted(Comparator.comparing(
                        one -> !Boolean.TRUE.equals(one.getPrimaryContact())))
                .map(InquiryGuardian::getPhoneNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * What to do with this lead next, in plain words.
     *
     * <p><b>Private and static, not in {@code utils}.</b> It reads no repository — it is a
     * sentence about a document — and the folder rules send shared <i>reads</i> there. The other
     * two private helpers beside it are the same shape.
     *
     * Used by:
     * - createInquiry()
     * - updateInquiry()
     * - getInquiry()
     */
    private static String nextStepFor(Inquiry inquiry) {
        String assigned = inquiry.getAssignedCounselorDocsId() == null
                ? " Nobody is looking after it yet — #11 gives it to a counsellor, and is not "
                        + "built."
                : " It is somebody's to chase.";

        return "Captured as NEW."
                + assigned
                + " #10 logs what happens on a call and #12 moves it along; neither is built, so "
                + "this lead cannot move from NEW yet. What CAN happen to it is an application: "
                + "#17 takes one naming this lead and moves it to APPLICATION_STARTED, and #19 to "
                + "APPLICATION_SUBMITTED — those are the only two statuses anything writes today.";
    }
}
