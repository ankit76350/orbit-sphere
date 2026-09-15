package com.orbitastra.backend.services.people;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.people.staff.request.EmploymentCreateRequest;
import com.orbitastra.backend.dto.people.staff.request.StaffCreateRequest;
import com.orbitastra.backend.dto.people.staff.request.StaffSearchRequest;
import com.orbitastra.backend.dto.people.staff.response.EmploymentResponse;
import com.orbitastra.backend.dto.people.staff.response.EmploymentWriteResponse;
import com.orbitastra.backend.dto.people.staff.response.StaffCreatedResponse;
import com.orbitastra.backend.dto.people.staff.response.StaffDetailResponse;
import com.orbitastra.backend.dto.people.staff.response.StaffRowResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.people.staff.EmploymentRecord;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.models.people.staff.enums.EmploymentStatus;
import com.orbitastra.backend.models.people.organization.Position;
import com.orbitastra.backend.repositories.people.organization.PositionRepository;
import com.orbitastra.backend.repositories.people.staff.EmploymentRecordRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.institution.NumberSequenceService;
import com.orbitastra.backend.services.people.utils.StaffServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * The people a school employs — endpoints #1 to #8 of the plan in
 * {@code controllers/people/staff/README.md}. #1, #7, #8 and #16 are built.
 *
 * <p><b>The person and the job are two documents, and that is the whole design.</b> {@code Staff}
 * has no status, no department, no designation and no joining date. Every field on it is a fact
 * about a human being; everything about the employment lives on {@code EmploymentRecord}, which
 * #16 writes. Somebody created here and not yet hired is a real state, not a half-finished one.
 *
 * <p><b>This is the second half of the module's phase 1.</b> #9 and #13 built the seat; this
 * builds the person. #16 is what joins them, and five other modules are waiting on the
 * {@code staffDocsId} this hands back.
 *
 * <p><b>The gates run in the controller</b>, not here.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final CurrentSchoolResolver currentSchool;
    private final StaffRepository staff;
    private final EmploymentRecordRepository employments;
    private final PositionRepository positions;
    private final NumberSequenceService numberSequences;
    private final StaffServiceUtils utils;

    /**
     * What #7 may be ordered by, keyed by the lower-cased name a caller sends.
     *
     * <p><b>An allowlist, not a pass-through.</b> A sort field taken straight from the query
     * string is a way to order by anything on the document — including fields a row deliberately
     * does not return, which leaks their values through the ordering.
     */
    private static final Map<String, String> SORTABLE_STAFF_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_STAFF_FIELDS.put("fullname", "fullName");
        SORTABLE_STAFF_FIELDS.put("employeeno", "employeeNo");
        SORTABLE_STAFF_FIELDS.put("createdat", "createdAt");
        SORTABLE_STAFF_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_STAFF_FIELD_NAMES =
            SORTABLE_STAFF_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order, and the tiebreaker on every other sort.
     *
     * <p><b>Two keys, because the first is not unique.</b> Two people genuinely share a name — it
     * is the most ordinary thing in a school roll — so {@code fullName} alone ties, and a tie with
     * no tiebreaker puts one row on two pages while another appears on none. {@code employeeNo} is
     * unique per school by index and settles it.
     *
     * <p>{@link PageResponse#pageableOf} appends whichever of these the caller did not name, so
     * {@code ?sort=createdAt} is really {@code createdAt, fullName, employeeNo}.
     */
    private static final Sort STAFF_ORDER =
            Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("employeeNo"));

    /**
     * Why a person comes back with no employment block.
     *
     * <p>On the response rather than only in a README, because the absence is the kind of thing a
     * caller otherwise reads as a bug in their own code.
     *
     * <p>Rewritten 2026-09-15 when #16 was built: it used to say nobody in the product was
     * employed anywhere, which stopped being true the moment #16 ran.
     */
    private static final String NOT_EMPLOYED =
            "This person has no current employment record, which is a real state rather than a "
                    + "missing one — somebody the school has entered and not yet hired. #16 "
                    + "POST /staff/{id}/employment is what employs them, and #17 is what returns "
                    + "them here.";

    private static final String NO_AUTHORIZATION_YET =
            "This module has no authorization yet, so treat every field on it as readable by "
                    + "anybody who can reach the API.";

    /**
     * Endpoint #1 — create a person.
     *
     * <p><b>{@code employeeNo} is generated, never accepted.</b> Two schools' numbering
     * conventions must not be able to collide inside one tenant, and nobody should pick their own
     * staff number. The sequence is allocated atomically, so two simultaneous creates cannot be
     * handed the same one.
     *
     * <p><b>A phone number and an email address are both required, and each identifies one person
     * within a school.</b> The module plan said {@code fullName} was the only required field and
     * that a duplicate email was fine — "two staff genuinely may share a family address". Both
     * were overruled on 2026-09-15: a staff record with no way to contact the person is one the
     * office has to chase later.
     */
    public StaffCreatedResponse createStaff(StaffCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the phone and the email, normalised BEFORE they are checked.
        //!
        //! NORMALISE FIRST OR THE CHECK IS A LIE. "+91 98765-43210" and "+919876543210" are one
        //! number and "Anita@X.com" and "anita@x.com" are one address - checking the raw strings
        //! would pass both, store both, and leave the school with a duplicate the index would
        //! have refused had it ever been built.
        String phoneNumber = utils.normalisePhone(request.phoneNumber());
        String emailAddress = TextHelper.lowercaseOrNull(request.emailAddress());

        //! BOTH ARE REQUIRED, and @NotBlank is not quite enough for the phone: "---" and "( )"
        //! pass it and then normalise away to nothing, which would store a blank number behind a
        //! request that looked valid.
        if (phoneNumber == null) {
            throw ApiException.badRequest("STAFF_PHONE_REQUIRED",
                    "A phone number is required, and '" + request.phoneNumber() + "' has no "
                            + "digits in it. Spacing characters are stripped, so a number made "
                            + "only of them is no number at all.");
        }

        //! step 3 - and each has to be free.
        //!
        //! THESE CHECKS ARE THE ENFORCEMENT, not a nicety in front of the indexes.
        //! school_staff_phone_uniq and school_staff_email_uniq are declared on the model but built
        //! on demand (app.mongo.sync-indexes), so a database that has never synced carries no such
        //! constraint at all - where they ARE built, this turns a duplicate-key 500 into a 409
        //! that says which field and what to do.
        //!
        //! BOTH INDEXES ARE PARTIAL, so a person with no phone and no email is always allowed:
        //! only a value that is actually there has to be unique.
        if (phoneNumber != null) {
            // TODO: check staff exists
            if (staff.existsBySchoolIdAndPhoneNumber(school.getId(), phoneNumber)) {
                throw ApiException.conflict("STAFF_PHONE_TAKEN",
                        "Somebody in this school already has the phone number " + phoneNumber
                                + ". A number identifies one person here — check whether they are "
                                + "already on the books before entering them again.");
            }
        }

        if (emailAddress != null) {
            // TODO: check staff exists
            if (staff.existsBySchoolIdAndEmailAddress(school.getId(), emailAddress)) {
                throw ApiException.conflict("STAFF_EMAIL_TAKEN",
                        "Somebody in this school already has the email address " + emailAddress
                                + ". An address identifies one person here — check whether they "
                                + "are already on the books before entering them again.");
            }
        }

        //! step 4 - take an employee number. ATOMIC, so two requests can never be handed the same
        //! one - see NumberSequenceService. The counter itself was created when the school was
        //! provisioned, with no template; the first caller's template is written onto it and every
        //! later number in the school's life reads the same shape.
        //!
        //! "EMP/{YYYY}/{MM}/" with the counter's width of 6 gives EMP/2026/09/000001.
        //!
        //! THE SHAPE IS FIXED ONCE PER SCHOOL, not once per release. A school that already
        //! allocated a number under an older template keeps it - changing this line does not
        //! restyle numbers that have already been written down, and it must not.
        String employeeNo = numberSequences.next(school.getId(),
                NumberSequenceType.EMPLOYEE_NUMBER, "EMP/{YYYY}/{MM}/");

        //! step 5 - build the person.
        //!
        //! schoolId IS SET EXPLICITLY, and that is not boilerplate. SchoolBase declares it
        //! @NotBlank but nothing validates a document on save: a person written without it is
        //! stored, invisible to every tenant-scoped query, and found only by reading the raw
        //! collection. That exact bug shipped in this project's term create on 2026-09-11.
        //!
        //! NO active, NO status, NO department, NO joining date - there are none on the document.
        //! A person is not employed by existing; #16 writes the job.
        Staff person = Staff.builder()
                .schoolId(school.getId())
                .employeeNo(employeeNo)
                .fullName(request.fullName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .nationalityCode(TextHelper.uppercaseOrNull(request.nationalityCode()))
                .preferredLanguage(TextHelper.blankToNull(request.preferredLanguage()))
                .phoneNumber(phoneNumber)
                .emailAddress(emailAddress)
                .currentAddress(utils.toAddress(request.currentAddress()))
                .permanentAddress(utils.toAddress(request.permanentAddress()))
                .emergencyContact(utils.toEmergencyContact(request.emergencyContact()))
                .profileImageDocsId(TextHelper.blankToNull(request.profileImageDocsId()))
                .build();

        //! step 6 - insert it
        // TODO: insert staff
        Staff saved = staff.save(person);

        return StaffCreatedResponse.fromStaff(saved,
                "This person exists but is not employed yet — #16 writes the job, and needs "
                        + "staffDocsId " + saved.getId() + " and a positionDocsId. "
                        + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #16 — hire, promote or transfer.
     *
     * <p><b>One endpoint because it is one event.</b> All three close whichever record was current
     * and open a new one. Three endpoints doing that would be three chances to leave two records
     * current — or none, which is worse, because the person then reads as unemployed.
     *
     * <p><b>{@code @Transactional}, and that is the whole reason this is safe.</b> Two documents
     * move together: the old record is closed and the new one inserted. The module plan says this
     * project configures no transaction manager and that the close should therefore ride on the
     * previous record's {@code version} — <b>that is out of date</b>. {@code MongoTransactionConfig}
     * registers a {@code MongoTransactionManager}, and Atlas is a replica set, so the two writes
     * commit together or neither does.
     *
     * <p><b>The order is forced by the index.</b> {@code school_staff_current_employment_uniq} is
     * unique and partial on {@code current: true}, so a new current record cannot be inserted
     * while the old one still is. Close first, insert second, both inside the transaction.
     *
     * <p><b>Overlap with non-current records is not checked</b> — the module plan's open item 1
     * settles on allowing it: a part-time music teacher who also runs the choir on a separate
     * contract is real, and "current" then means the post the school considers primary.
     */
    @Transactional
    public EmploymentWriteResponse employStaff(String staffDocsId,
            EmploymentCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - a record that is current AND terminal is the contradiction open item 2 warns
        //! about. Nothing in the model stops it, so this does. Leaving is #17.
        if (request.status() == EmploymentStatus.TERMINATED) {
            throw ApiException.badRequest("EMPLOYMENT_STATUS_TERMINAL",
                    "A record cannot be created already TERMINATED — it would be current and "
                            + "finished at the same time, which nothing downstream can read. "
                            + "Ending an employment is #17 POST /staff/{id}/separate.");
        }

        //! step 3 - the person, scoped to the school
        String personId = staffDocsId == null ? "" : staffDocsId.trim();
        // TODO: read staff
        Staff person = staff.findByIdAndSchoolId(personId, school.getId())
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                        "No staff member with id '" + personId + "' in this school."));

        //! step 4 - the seat, scoped the same way
        String seatId = request.positionDocsId().trim();
        // TODO: read position
        Position seat = positions.findByIdAndSchoolId(seatId, school.getId())
                .orElseThrow(() -> ApiException.notFound("POSITION_NOT_FOUND",
                        "No position with id '" + seatId + "' in this school."));

        //! step 5 - and it has to still be one. Employing somebody into a retired seat leaves a
        //! person whose job does not appear on any chart.
        if (!Boolean.TRUE.equals(seat.getActive())) {
            throw ApiException.conflict("POSITION_NOT_ACTIVE",
                    "'" + seat.getTitle() + "' is retired, so nobody can be employed into it. "
                            + "Restore the position first, or use the one that replaced it.");
        }

        //! step 6 - the manager, when one was named. A PERSON, not a seat: Position carries
        //! reportsToPositionDocsId for the structural question, and this answers "who do I
        //! actually report to", which is why both exist.
        String managerId = TextHelper.blankToNull(request.managerDocsId());
        if (managerId != null) {
            if (managerId.equals(person.getId())) {
                throw ApiException.badRequest("MANAGER_IS_SELF",
                        "Somebody cannot be their own manager.");
            }
            // TODO: read staff
            staff.findByIdAndSchoolId(managerId, school.getId())
                    .orElseThrow(() -> ApiException.notFound("MANAGER_NOT_FOUND",
                            "No staff member with id '" + managerId + "' in this school to "
                                    + "manage them."));
        }

        //! step 7 - probation cannot end before the employment starts.
        LocalDate from = request.effectiveFrom();
        if (request.probationUntil() != null && request.probationUntil().isBefore(from)) {
            throw ApiException.badRequest("PROBATION_BEFORE_START",
                    "Probation ends " + request.probationUntil() + ", before the employment "
                            + "starts on " + from + ".");
        }

        //! step 8 - two records cannot START on the same day. This is the check in front of
        //! school_staff_employment_start_uniq, and it covers CLOSED records too - somebody rehired
        //! on the exact day an old contract began is a real mistake, and the index refuses it
        //! either way. This turns a duplicate-key 500 into a 409 that says what happened.
        // TODO: check employment exists
        if (employments.existsBySchoolIdAndStaffDocsIdAndEffectiveFrom(
                school.getId(), person.getId(), from)) {

            throw ApiException.conflict("EMPLOYMENT_ALREADY_STARTS_THEN",
                    person.getFullName() + " already has an employment record beginning on "
                            + from + ". Correcting a date on a record already written is #18.");
        }

        //! step 9 - the record being displaced, if any.
        // TODO: read employment
        EmploymentRecord previous = employments
                .findBySchoolIdAndStaffDocsIdAndCurrentIsTrue(school.getId(), person.getId())
                .orElse(null);

        EmploymentResponse closed = null;
        if (previous != null) {
            //! THE NEW ONE MUST START AFTER THE OLD ONE DID, or the close below would set an end
            //! date before its own start and the history would read backwards.
            if (!from.isAfter(previous.getEffectiveFrom())) {
                throw ApiException.conflict("EMPLOYMENT_STARTS_BEFORE_CURRENT",
                        "This would start on " + from + ", on or before the current employment's "
                                + "own start of " + previous.getEffectiveFrom()
                                + " — which would end that record before it began. A correction "
                                + "to an existing record is #18.");
            }

            //! step 10 - close it. THE END IS COMPUTED, not sent: the day before the new one
            //! starts, so there is no gap and no overlap. Two people typing two dates is how
            //! either gets in.
            previous.setCurrent(false);
            previous.setEffectiveUntil(from.minusDays(1));

            // TODO: update employment
            EmploymentRecord closedRecord = employments.save(previous);
            closed = EmploymentResponse.fromRecord(closedRecord);
        }

        //! step 11 - build the new record
        EmploymentRecord opening = EmploymentRecord.builder()
                .schoolId(school.getId())
                .staffDocsId(person.getId())
                .positionDocsId(seat.getId())
                .managerDocsId(managerId)
                .status(request.status())
                .employmentType(request.employmentType())
                .effectiveFrom(from)
                .probationUntil(request.probationUntil())
                .current(true)
                .build();

        //! step 12 - open it. INSIDE THE SAME TRANSACTION as the close above: the unique partial
        //! index forbids two current records, so this order is the only one possible, and without
        //! the transaction a failure here would leave the person with NONE.
        // TODO: insert employment
        EmploymentRecord saved = employments.save(opening);

        //! step 13 - the headcount, COUNTED and never stored. A stored filledHeadcount drifts the
        //! first time a writer forgets it - the objection that also keeps a weight total off
        //! AcademicTerm.
        //!
        //! A WARNING, NOT A REFUSAL. A school hiring a twelfth teacher into eleven approved seats
        //! is describing something that has already happened, and refusing it stops the system
        //! recording the truth.
        // TODO: count employments
        long filled = employments.countBySchoolIdAndPositionDocsIdAndCurrentIsTrue(
                school.getId(), seat.getId());

        String warning = null;
        if (seat.getApprovedHeadcount() != null && filled > seat.getApprovedHeadcount()) {
            warning = "'" + seat.getTitle() + "' now holds " + filled + " people against an "
                    + "approved headcount of " + seat.getApprovedHeadcount()
                    + ". That is recorded, not refused — but the position count or the hiring plan "
                    + "is out of date. #14 raises the approved headcount.";
        }

        return new EmploymentWriteResponse(
                EmploymentResponse.fromRecord(saved),
                closed,
                warning,
                closed == null
                        ? person.getFullName() + " is now employed. " + NO_AUTHORIZATION_YET
                        : person.getFullName() + " moved to a different position; the previous "
                                + "record was closed on "
                                + from.minusDays(1) + ". " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #8 — one person in full.
     *
     * <p><b>The fullest thing this product returns about a human being</b> — a date of birth, two
     * addresses, an emergency contact. #7's row carries none of it precisely so that this one can:
     * a list is read by every dropdown, and this is read by one page.
     *
     * <p><b>The employment block is absent, and says why.</b> The plan folds the person's current
     * employment record in here, because "who is this and what do they do" is one question. #16
     * writes that record and is not built — there is no {@code employment_records} collection at
     * all — so the key is absent rather than null or an empty object.
     *
     * <p><b>That shape is not scaffolding.</b> A person with no employment record is a real state
     * even once #16 exists: somebody the school has entered and not yet hired, which is exactly
     * what #1 leaves them in. This is what that person will always look like.
     *
     * <p><b>No gate runs on it.</b> A suspended or closed school still reads its own people.
     */
    public StaffDetailResponse getStaff(String staffDocsId) {

        //! step 1 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own people.
        School school = currentSchool.require();

        //! step 2 - the person, scoped by schoolId and NEVER by id alone. Another school's real
        //! id is a real id, and an unscoped findById would hand one school another's staff - which
        //! on THIS endpoint means a date of birth, a home address and an emergency contact.
        String id = staffDocsId == null ? "" : staffDocsId.trim();
        // TODO: read staff
        Staff person = staff.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                        "No staff member with id '" + id + "' in this school."));

        //! step 3 - what they do here now, folded in rather than linked: "who is this and what
        //! do they do" is one question, and every caller would make the second call anyway.
        //!
        //! AT MOST ONE CAN COME BACK. school_staff_current_employment_uniq is unique and partial
        //! on current:true, so the database itself forbids a second.
        // TODO: read employment
        EmploymentResponse employment = employments
                .findBySchoolIdAndStaffDocsIdAndCurrentIsTrue(school.getId(), person.getId())
                .map(EmploymentResponse::fromRecord)
                .orElse(null);

        //! The note appears ONLY when there is nothing to fold in - a person entered and not yet
        //! hired, which is a real state and not a broken row.
        return StaffDetailResponse.fromStaff(person, employment,
                employment == null ? NOT_EMPLOYED : null, NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #7 — one page of the school's people.
     *
     * <p><b>The list behind every teacher picker</b> — eventually. The filters that make it one
     * live on {@code EmploymentRecord}, and #16 is what writes one: as of 2026-09-15 there is no
     * repository and no {@code employment_records} collection at all. What this answers today is
     * every question {@code staff} alone can answer, and the four employment filters arrive with
     * #16 rather than being accepted now and silently matching nothing.
     *
     * <p><b>The row is thin on purpose.</b> No date of birth, no address, no emergency contact —
     * those are #8, one call away. A list endpoint returning them puts every employee's personal
     * data in the network tab of every dropdown, and this module has no authorization yet.
     *
     * <p><b>No gate runs on it.</b> A suspended or closed school still reads its own staff list.
     */
    public PageResponse<StaffRowResponse> listStaff(StaffSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_STAFF_FIELDS, SORTABLE_STAFF_FIELD_NAMES, STAFF_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended or closed school
        //! can still read its own people.
        School school = currentSchool.require();

        //! step 3 - one page, filtered and ordered in the database
        // TODO: read staff
        return PageResponse.from(
                staff.search(school.getId(), request, pageable),
                // The single-argument factory, so no `nextStep` appears on a row: a read changed
                // nothing, and a null on every row is noise a client has to decide about.
                StaffRowResponse::fromStaff);
    }

}
