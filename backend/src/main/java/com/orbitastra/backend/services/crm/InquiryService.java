package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.orbitastra.backend.dto.crm.inquiry.request.InquiryStatusRequest;
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
            interested = utils.requireClassOfYear(school, classId, year, false);
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
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
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
            interested = utils.requireClassOfYear(school, wantedClass.trim(), year, false);
        } else if (request.academicYear() != null
                && inquiry.getInterestedClassDocsId() != null) {
            interested = utils.requireClassOfYear(school, inquiry.getInterestedClassDocsId(), year, true);
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

        //! step 9 - the class name, for the answer. TOLERANTLY, as everywhere here: a class that
        //! was removed is reported by leaving the name off rather than by refusing the correction.
        return InquiryResponse.fromInquiry(saved, utils.classNameOrNull(school, saved),
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
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
            Set<InquiryStatus> allowed = utils.allowedNext(inquiry.getStatus());

            if (!allowed.contains(moved)) {
                throw ApiException.conflict("INQUIRY_TRANSITION_NOT_ALLOWED",
                        "'" + inquiry.getProspectiveStudentName() + "' is " + inquiry.getStatus()
                                + " and cannot go to " + moved + ". It can go to: "
                                + utils.names(allowed) + ".");
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

        return utils.detailOf(school, saved, utils.overdueNow(saved),
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #12 — <b>move the lead, and say why when it is being given up on</b>.
     *
     * <p><b>#10 can move a lead too, and the split is the point.</b> #10 logs a call that
     * <i>happened to</i> move it. This is the move on its own — the school writing a family off in
     * January because nobody has answered since October, where there was no call and pretending
     * there was one to record the outcome would put a fiction in the timeline.
     *
     * <p><b>This is the only thing that may set {@code LOST}</b>, because it is the only one with
     * somewhere to put the reason. A lead marked lost with no reason is a record that answers
     * nothing, and <i>why</i> is the only question anybody asks of one six months later.
     *
     * <p><b>It still cannot set the two the application half owns</b>, and refuses them with the
     * same code #10 does.
     *
     * <p><b>Every move lands on the timeline</b>, whether a note was sent or not. A history that
     * showed every phone call but not the moment a family was written off would be misleading
     * about the one thing that matters most.
     *
     * <p><b>And every move ends the chasing.</b> {@code nextFollowUpAt} is cleared — nobody owes a
     * call to a family that has gone elsewhere, and leaving the date would keep the lead on #13's
     * overdue worklist for ever.
     *
     * <p><b>Two gates.</b> A write.
     */
    public InquiryDetailResponse moveStatus(String inquiryId, InquiryStatusRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        log.info("[moveStatus] Step 1: Moving lead {} to {} for school {}", inquiryId,
                request.status(), school.getId());

        //! step 2 - the lead, scoped by school in the QUERY.
        Inquiry inquiry = utils.loadInquiry(school, inquiryId);

        //! step 3 - somebody else may have moved it while this caller was reading.
        if (request.version() != null && !request.version().equals(inquiry.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + inquiry.getProspectiveStudentName() + "' changed since you read it. "
                            + "Read it again before moving it.");
        }

        InquiryStatus moved = request.status();

        //! step 4 - the two nobody may type. The same check #10 makes, and checked BEFORE the
        //! table for the same reason: the table permits them, so "it cannot go there" would be a
        //! lie. What the caller needs to know is WHO does set them.
        if (NOT_BY_HAND.contains(moved)) {
            throw ApiException.conflict("INQUIRY_STATUS_NOT_BY_HAND",
                    moved + " is a fact about an application, not something a counsellor may set. "
                            + "#17 sets it when a form is started and #19 when it is submitted — "
                            + "otherwise a lead could claim a form that does not exist.");
        }

        //! step 5 - the reason, which LOST needs and nothing else may carry.
        //!
        //! REFUSED RATHER THAN DROPPED when it comes with another status. A reason attached to a
        //! move that is not a loss is a caller who has misunderstood something, and silence would
        //! let them go on believing it.
        String lostReason = TextHelper.blankToNull(request.lostReason());

        if (moved == InquiryStatus.LOST && lostReason == null) {
            throw ApiException.badRequest("LOST_REASON_REQUIRED",
                    "Giving up on a lead needs a reason. 'Why' is the only question anybody asks "
                            + "of a lost lead six months later, and LOST on its own is the one "
                            + "thing that cannot answer it.");
        }
        if (moved != InquiryStatus.LOST && lostReason != null) {
            throw ApiException.badRequest("LOST_REASON_NOT_ALLOWED",
                    "A lostReason only belongs on a move to LOST, and this one is to " + moved
                            + ". Send the words as a note instead — they will land on the "
                            + "timeline either way.");
        }

        //! step 6 - the table, which is the product rule.
        //!
        //! A MOVE TO WHERE IT ALREADY IS IS REFUSED HERE, and accepted by #10. The difference is
        //! deliberate: #10's status is a detail of a call that did happen, so echoing the current
        //! one is harmless. This endpoint's whole job is the move, and a request that moves
        //! nothing has asked for nothing.
        Set<InquiryStatus> allowed = utils.allowedNext(inquiry.getStatus());

        if (!allowed.contains(moved) || moved == inquiry.getStatus()) {
            throw ApiException.conflict("INQUIRY_TRANSITION_NOT_ALLOWED",
                    "'" + inquiry.getProspectiveStudentName() + "' is " + inquiry.getStatus()
                            + " and cannot go to " + moved + ". It can go to: " + utils.names(allowed)
                            + ".");
        }

        //! step 7 - who moved it, when the caller says. Same shape as #10's.
        String counselorId = TextHelper.blankToNull(request.counselorDocsId());

        if (counselorId != null) {
            // TODO: read staff
            staff.findByIdAndSchoolId(counselorId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                            "No staff member with id '" + counselorId + "' in this school, so the "
                                    + "move cannot be recorded against them."));
        }

        //! step 8 - the entry that records the move.
        //!
        //! THE NOTE FALLS BACK TO THE REASON on a loss, because the reason is almost always the
        //! sentence somebody would have typed. It falls back to NOTHING otherwise rather than to
        //! an invented sentence: #14 renders a null note as "nothing written", which is true, and
        //! "Moved to CLOSED" would be the row repeating its own status column back at itself.
        String note = TextHelper.blankToNull(request.note());

        InquiryFollowUp entry = InquiryFollowUp.builder()
                .status(moved)
                .note(note != null ? note : lostReason)
                .communicationChannel(null)
                .nextFollowUpAt(null)
                .counselorDocsId(counselorId)
                .recordedAt(Instant.now())
                .build();

        //! step 9 - one atomic update: the move, the reason, the entry, and the end of chasing.
        // TODO: update inquiry (move its status)
        long moveCount = inquiries.moveStatus(school.getId(), inquiry.getId(), moved, lostReason,
                entry, request.version());

        //! NOTHING MOVED means the lead went, or somebody won the race. Re-reading is what tells
        //! those two apart, and "it is gone" and "you were too slow" are different things to be
        //! told.
        if (moveCount == 0) {
            Inquiry now = inquiries.findByIdAndSchoolId(inquiry.getId(), school.getId())
                    .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND",
                            "No inquiry with id '" + inquiry.getId() + "' in this school."));

            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + now.getProspectiveStudentName() + "' changed while this was being "
                            + "written. Read it again before moving it.");
        }
        log.info("[moveStatus] Step 2: Lead {} is now {}", inquiry.getId(), moved);

        //! step 10 - read it back, so the caller sees the timeline the move landed on.
        Inquiry saved = utils.loadInquiry(school, inquiry.getId());

        return utils.detailOf(school, saved, utils.overdueNow(saved),
                utils.nextStepFor(saved) + " " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #13 — <b>the counsellor's worklist</b>.
     *
     * <p><b>Soonest to chase first</b>, which is the whole of what a worklist is. {@code status}
     * plus {@code nextFollowUpAt} are exactly {@code school_inquiry_pipeline_idx}, which exists
     * for this.
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

        //! step 3 - thin rows.
        //!
        //! NO STAFF QUERY ANY MORE. A page used to resolve its counsellors' names in one go;
        //! leads are not owned by anybody since #11 and assignedCounselorDocsId were removed
        //! together, so there is nobody to name and the read is one query rather than two.
        return PageResponse.from(found, one -> InquirySummaryResponse.fromInquiry(one,
                utils.contactNumberOf(one),
                utils.overdueNow(one)));
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

        //! step 3 - the answer, built where #10's and #12's are built.
        //!
        //! IT BUILT ITS OWN UNTIL 2026-09-24, with a comment claiming the duplicate was
        //! deliberate because this endpoint had READ a lead and the other two had WRITTEN to one.
        //! That distinction bought nothing — the response is the same response — and it left two
        //! places for the same two queries to drift apart.
        return utils.detailOf(school, inquiry, utils.overdueNow(inquiry),
                utils.nextStepFor(inquiry) + " " + NO_AUTHORIZATION_YET);
    }

}
