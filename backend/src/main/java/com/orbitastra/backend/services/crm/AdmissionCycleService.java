package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.common.time.SchoolZone;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleCapacitiesRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleCreateRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleSearchRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleStatusRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleUpdateRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleCapacityResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleDetailResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.ClassStatusCount;
import com.orbitastra.backend.repositories.crm.admissioncycle.AdmissionCycleRepository;
import com.orbitastra.backend.services.crm.helper.CrmHelper;
import com.orbitastra.backend.services.crm.utils.AdmissionCycleServiceUtils;

import com.orbitastra.backend.common.time.AcademicYearWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admission cycles — one round of admissions for one academic year. Endpoints #1 to #6 of the
 * plan in {@code controllers/crm/README.md}, except #7; the whole cycle half of the module.
 *
 * <p>School surface, so the school comes from CurrentSchoolResolver and never from the URL. There
 * is no platform surface for cycles: no operator of ours decides when a school admits students.
 *
 * <p><b>This is the first call anybody makes in this module.</b> Every application has to name a
 * cycle, so nothing else here works until one exists.
 *
 * <p><b>The academic year does not have to be the running one.</b> Every other module refuses a
 * write against a year the school is not currently running. This module is the opposite case: a
 * school sets up its 2027-2028 admissions in the middle of 2026-2027, and often runs two cycles at
 * once — late admissions into this year while next year's cycle is open. The controller therefore
 * runs gates 1 and 2 and not gate 4. The full reasoning is in the README.
 *
 * <p><b>There is still no utils file for this module.</b> #1 and #5 share nothing: the year check
 * and the date ordering are #1's alone, and the paging is #5's. This project's rule is that logic
 * with one caller stays inline under its own step. When #2 and #3 arrive, the year check and the
 * date ordering get a second caller and move to {@code utils/AdmissionCycleServiceUtils.java}
 * then — not before.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdmissionCycleService {

    private final AdmissionCycleRepository admissionCycles;
    //! #7 COUNTS APPLICATIONS, which is the one thing this service reads outside its own
    //! collection — and it reads them as a grouped total rather than as rows.
    private final AdmissionApplicationRepository applications;
    private final AcademicYearRepository academicYears;
    private final AcademicYearWindow yearWindow;
    private final AdmissionCycleServiceUtils utils;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolZone schoolZone;
    private final CrmHelper helper;

    /**
     * The fields #5 may be ordered by: what a caller types -> the field on the document.
     *
     * <p><b>This is a security control, not a convenience.</b> Without it a caller can order by
     * any field the document holds, and ordering is a read: sorting by a field and walking the
     * pages tells you its values even when nothing shows them.
     *
     * <p>Keys are lower case because a caller should not have to guess the casing.
     * {@code capacities} is deliberately absent — sorting by an array orders on its first element
     * in Mongo, which would sort cycles by whichever class happened to be entered first, a result
     * that looks deliberate and means nothing.
     */
    private static final Map<String, String> SORTABLE_CYCLE_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_CYCLE_FIELDS.put("name", "name");
        SORTABLE_CYCLE_FIELDS.put("academicyear", "academicYear");
        SORTABLE_CYCLE_FIELDS.put("status", "status");
        SORTABLE_CYCLE_FIELDS.put("inquiryopenat", "inquiryOpenAt");
        SORTABLE_CYCLE_FIELDS.put("applicationopenat", "applicationOpenAt");
        SORTABLE_CYCLE_FIELDS.put("applicationcloseat", "applicationCloseAt");
        SORTABLE_CYCLE_FIELDS.put("enrollmentdeadlineat", "enrollmentDeadlineAt");
        SORTABLE_CYCLE_FIELDS.put("createdat", "createdAt");
        SORTABLE_CYCLE_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_CYCLE_FIELD_NAMES =
            SORTABLE_CYCLE_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order: newest year first, then the rounds inside it by name.
     *
     * <p><b>It is also the tiebreaker on every other sort.</b> {@link PageResponse#pageableOf}
     * appends the fallback to whatever the caller named, minus any key they already used — so
     * {@code ?sort=status} is really {@code status, academicYear desc, name}.
     *
     * <p><b>Which makes the choice of fallback the decision that matters.</b> The pair is unique
     * within a school: {@code school_academic_year_cycle_name_uniq} declares it and #1 enforces
     * it. So every sort ends in a total order, and paging cannot show one row twice while never
     * showing another. Neither field alone would do — a school holds several rounds in one year,
     * and reuses one name across years.
     *
     * <p>Newest year first because the work is nearly always the year that has not started yet.
     */
    private static final Sort CYCLE_ORDER =
            Sort.by(Sort.Order.desc("academicYear"), Sort.Order.asc("name"));

    private static final List<String> DATE_NAMES = List.of(
            "enquiries open", "applications open", "applications close", "the enrollment deadline");

    /**
     * Where a cycle may go from where it is. The graph in the module's README, as code.
     *
     * <pre>
     * DRAFT ──> SCHEDULED ──> OPEN ──> CLOSED ──> COMPLETED
     *   │           │           │         │
     *   └───────────┴───────────┴─────────┴──> CANCELLED
     * </pre>
     *
     * <p><b>It only goes forwards.</b> A cycle that was closed by mistake cannot be reopened, and
     * that is the graph's decision rather than an oversight — see the open item in the README. The
     * safe undo is a new cycle, which costs a name and nothing else.
     *
     * <p><b>Both ends are terminal.</b> COMPLETED is where a finished round stops; CANCELLED is
     * reachable from anywhere before it, because a school can abandon a round at any point.
     */
    private static final Map<AdmissionCycleStatus, Set<AdmissionCycleStatus>> CYCLE_MOVES =
            new EnumMap<>(AdmissionCycleStatus.class);

    static {
        CYCLE_MOVES.put(AdmissionCycleStatus.DRAFT, EnumSet.of(
                AdmissionCycleStatus.SCHEDULED, AdmissionCycleStatus.OPEN,
                AdmissionCycleStatus.CANCELLED));
        CYCLE_MOVES.put(AdmissionCycleStatus.SCHEDULED, EnumSet.of(
                AdmissionCycleStatus.OPEN, AdmissionCycleStatus.CANCELLED));
        CYCLE_MOVES.put(AdmissionCycleStatus.OPEN, EnumSet.of(
                AdmissionCycleStatus.CLOSED, AdmissionCycleStatus.CANCELLED));
        CYCLE_MOVES.put(AdmissionCycleStatus.CLOSED, EnumSet.of(
                AdmissionCycleStatus.COMPLETED, AdmissionCycleStatus.CANCELLED));
        //! TERMINAL, and spelled out rather than left missing. An absent key and an empty set
        //! mean the same thing to the code, but only one of them says it was decided.
        CYCLE_MOVES.put(AdmissionCycleStatus.COMPLETED, EnumSet.noneOf(AdmissionCycleStatus.class));
        CYCLE_MOVES.put(AdmissionCycleStatus.CANCELLED, EnumSet.noneOf(AdmissionCycleStatus.class));
    }

    /**
     * Endpoint #1 — sets up a new admission cycle for one academic year.
     *
     * <p>The cycle is created <b>empty and not started</b>: status DRAFT, and no seats. Seats are
     * added by #4 and the cycle is opened by #3.
     *
     * <pre>
     * 404 ACADEMIC_YEAR_NOT_FOUND   no year with that name in this school
     * 409 CYCLE_NAME_TAKEN          that year already has a cycle with that name
     * 400 CYCLE_DATES_OUT_OF_ORDER  the dates given are not in a sensible order
     * </pre>
     */
    public AdmissionCycleResponse createCycle(AdmissionCycleCreateRequest request) {
        log.info("[createCycle] Step 1: Finding out which school is asking");
        //! step 1 - who is asking. requireUsable and not require, because this is a write.
        //!
        //! NOT REACHABLE AS A REFUSAL, and kept anyway. Mutation M12 swapped this for require()
        //! and every test still passed, because the controller has already run
        //! gate.requireActiveSchool - which is stricter than this check, not weaker: it allows
        //! only ACTIVE, while requireUsable also allows PROVISIONING. So the gate always says no
        //! first. This stays because every other service in the project does the same, and
        //! because it is what protects the method if it is ever called from somewhere that
        //! forgot the gate.
        School school = currentSchool.requireUsable();

        //! step 2 - the year has to be one this school actually has.
        //! We do NOT check that it is the year the school is running. Admissions are set up for a
        //! year that has not started yet, so that check would refuse the normal case.
        String year = request.academicYear() == null ? "" : request.academicYear().trim();
        log.info("[createCycle] Step 2: Checking the school has an academic year called '{}'", year);

        // TODO: check academic year exists
        if (!academicYears.existsBySchoolIdAndName(school.getId(), year)) {
            throw ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                    "No academic year called '" + year + "' in this school.");
        }

        //! step 3 - the name has to be free inside that year. A school can run more than one
        //! round for a year, so the name is the only thing staff have to tell two cycles apart.
        //! school_academic_year_cycle_name_uniq also stops it; this check is what turns a
        //! duplicate key error into a message that says which cycle already has the name.
        String name = request.name().trim();
        log.info("[createCycle] Step 3: Checking '{}' does not already have a cycle called '{}'",
                year, name);

        // TODO: check admission cycle exists
        if (admissionCycles.existsBySchoolIdAndAcademicYearAndName(school.getId(), year, name)) {
            throw ApiException.conflict("CYCLE_NAME_TAKEN",
                    "'" + year + "' already has an admission cycle called '" + name + "'.");
        }

        //! step 4 - the four dates have to run forwards.
        //! All four are optional, because a school often creates the cycle before it has settled
        //! its calendar. We only compare the ones that were actually sent, so a school can give
        //! just the application window now and fill the rest in later through #2.
        log.info("[createCycle] Step 4: Checking the dates that were given run in order");
        SchoolTimeZone zone = schoolZone.of(school);
        Instant[] dates = {
                request.inquiryOpenAt(),
                request.applicationOpenAt(),
                request.applicationCloseAt(),
                request.enrollmentDeadlineAt()
        };
        String[] dateNames = {
                "enquiries open",
                "applications open",
                "applications close",
                "the enrollment deadline"
        };

        //! step 4a - INSIDE THE YEAR THE ROUND ADMITS FOR, checked BEFORE the order.
        //!
        //! A cycle for 2026-2027 carrying a 2099 deadline or a 2019 inquiry date was possible
        //! until this, and both were in the database when it was written.
        //!
        //! BEFORE THE ORDER CHECK, and that ordering is load-bearing. A MIDDLE date outside the
        //! year is necessarily out of order too — anything earlier than the year's start is before
        //! the date above it, anything later is after the one below — so with the order check
        //! first, three of the four fields could only ever report CYCLE_DATES_OUT_OF_ORDER. The
        //! caller would be told to reorder dates whose real problem is that they are in the wrong
        //! YEAR. Measured: the suite could not prove the middle two were checked at all.
        //!
        //! IT RE-READS THE YEAR rather than reusing step 2's existence check, because it needs the
        //! start and end rather than a yes. One extra query on the cheapest write in the module.
        Map<String, Instant> sent = new LinkedHashMap<>();
        for (int i = 0; i < dates.length; i++) {
            if (dates[i] != null) {
                sent.put(AdmissionCycleServiceUtils.DATE_FIELDS.get(i), dates[i]);
            }
        }
        yearWindow.requireInside(school, year, zone, "CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR",
                sent);

        //! step 4b - and then they have to run forwards.
        Instant earlier = null;
        String earlierName = null;
        for (int i = 0; i < dates.length; i++) {
            if (dates[i] == null) {
                continue;
            }
            if (earlier != null && dates[i].isBefore(earlier)) {
                throw ApiException.badRequest("CYCLE_DATES_OUT_OF_ORDER",
                        "The dates are in the wrong order: " + dateNames[i] + " is "
                                + Dates.readable(dates[i], zone) + ", which is before "
                                + earlierName + " at " + Dates.readable(earlier, zone) + ".");
            }
            earlier = dates[i];
            earlierName = dateNames[i];
        }

        //! step 5 - build the cycle, with nothing set up in it yet.
        //! Seats are never set here. They are their own endpoint (#4), and mixing them in meant
        //! one request that could fail for two unrelated reasons - a name that is taken, or a bad
        //! seat row - with the caller having to work out which.
        //! schoolId is set by hand. Nothing fills it in for us, and a cycle saved without it
        //! belongs to no school and is invisible to every read.
        log.info("[createCycle] Step 5: Getting the new cycle ready to save");
        AdmissionCycle cycle = AdmissionCycle.builder()
                .schoolId(school.getId())
                .academicYear(year)
                .name(name)
                .status(AdmissionCycleStatus.DRAFT)
                .inquiryOpenAt(request.inquiryOpenAt())
                .applicationOpenAt(request.applicationOpenAt())
                .applicationCloseAt(request.applicationCloseAt())
                .enrollmentDeadlineAt(request.enrollmentDeadlineAt())
                .capacities(new ArrayList<>())
                .notes(TextHelper.blankToNull(request.notes()))
                .build();

        //! step 6 - save
        // TODO: insert admission cycle
        AdmissionCycle saved = admissionCycles.save(cycle);
        log.info("[createCycle] Step 6: Saved the cycle (id={}) as a DRAFT", saved.getId());

        return AdmissionCycleResponse.fromCycle(saved,
                "'" + saved.getName() + "' is a DRAFT with no seats set up yet. Set the seats per "
                        + "class next, then open the cycle — applications can only be taken once "
                        + "it is OPEN. " + AdmissionCycleServiceUtils.NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #5 — one page of this school's admission cycles.
     *
     * <p>Filter by year and by status, search the name, and ask which rounds were taking
     * applications on a given day. Every filter is optional; sending none returns the school's
     * cycles, newest year first.
     *
     * <pre>
     * 400 INVALID_PAGE     a negative page
     * 400 INVALID_PAGE_SIZE      a size below 1 or above the cap
     * 400 INVALID_SORT_FIELD     a field that is not in the allowlist
     * 400 INVALID_SORT_DIRECTION a direction that is not asc or desc
     * </pre>
     *
     * <p><b>No gates.</b> Reads run none, so a suspended school still sees the rounds it ran —
     * the children it admitted are still admitted.
     */
    public PageResponse<AdmissionCycleSummaryResponse> listCycles(
            AdmissionCycleSearchRequest request) {

        //! step 1 - the paging and the order, checked before anything is read. Cheap checks with
        //! no database behind them go first, so a bad sort costs no round trip.
        log.info("[listCycles] Step 1: Checking the paging and the sort order");
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_CYCLE_FIELDS, SORTABLE_CYCLE_FIELD_NAMES, CYCLE_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: this is a read, and a school
        //! that cannot be edited can still look at its own admissions.
        School school = currentSchool.require();
        log.info("[listCycles] Step 2: Reading the admission cycles for school {}", school.getId());

        //! step 3 - the search. The school id is passed in and never taken from the request: it
        //! is the tenant boundary, and a caller who could set it could read another school.
        // TODO: search admission cycles
        Page<AdmissionCycle> found = admissionCycles.search(school.getId(), request, pageable);
        log.info("[listCycles] Step 3: Found {} cycle(s) in total", found.getTotalElements());

        //! step 4 - hand back the thin rows. The notes and the seat table are on #6.
        return PageResponse.from(found, AdmissionCycleSummaryResponse::fromCycle);
    }

    /**
     * Endpoint #6 — one cycle in full, with its seat table.
     *
     * <p>What this adds over a row of #5: the {@code notes}, and the seat table itself instead of
     * a count of it. <b>Each seat row carries the class's name</b>, resolved here, because a table
     * of raw document ids is not something anybody can read.
     *
     * <p><b>It does NOT say how the seats are doing.</b> Offered, accepted, enrolled, free — those
     * are counted from {@code admission_applications} and they are #7. This reads one document and
     * reports what the school configured.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND  no cycle with that id in this school
     * </pre>
     *
     * <p><b>No gates.</b> Reads run none.
     */
    public AdmissionCycleDetailResponse getCycle(String admissionCycleId) {

        //! step 1 - who is asking. `require`, not `requireUsable`: a read, so a school that
        //! cannot be edited can still look at its own rounds.
        School school = currentSchool.require();
        String id = admissionCycleId == null ? "" : admissionCycleId.trim();
        log.info("[getCycle] Step 1: Reading cycle {} for school {}", id, school.getId());

        //! step 2 - the cycle, scoped by school in the QUERY. An id from another school is a real
        //! id: finding it first and checking the school afterwards would already have read it,
        //! and a "not found" that depends on remembering to check is one refactor from a leak.
        AdmissionCycle cycle = helper.loadCycle(school, id);

        //! step 3 - the seats, with the class names filled in.
        List<IntakeCapacity> seats = cycle.getCapacities() == null
                ? List.of()
                : cycle.getCapacities();

        //! ONE QUERY FOR EVERY CLASS, not one per seat row. A cycle can hold twenty classes, and
        //! reading them one at a time is the N+1 this project keeps naming.
        List<String> classIds = seats.stream()
                .map(IntakeCapacity::getClassDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        //! ASSIGNED ONCE, because the lambda below captures it. An empty seat table asks the
        //! database nothing, which is the common case rather than an edge one.
        Map<String, String> classNames =
                utils.classNamesFor(school, cycle.getAcademicYear(), classIds);
        log.info("[getCycle] Step 2: Named {} of {} class(es) in the seat table",
                classNames.size(), seats.size());

        //! step 4 - build the rows. A name that could not be found is left NULL rather than
        //! guessed: the cycle really does hold seats for a class this year no longer has, and
        //! inventing a name would hide it.
        Function<IntakeCapacity, AdmissionCycleDetailResponse.Seat> toSeat = seat ->
                new AdmissionCycleDetailResponse.Seat(
                        seat.getClassDocsId(),
                        seat.getClassDocsId() == null ? null : classNames.get(seat.getClassDocsId()),
                        seat.getTotalSeats(),
                        seat.getReservedSeats());

        List<AdmissionCycleDetailResponse.Seat> rows = seats.stream().map(toSeat).toList();

        //! step 5 - the total, added up here so every caller gets the same number.
        int total = rows.stream()
                .map(AdmissionCycleDetailResponse.Seat::totalSeats)
                .filter(each -> each != null)
                .mapToInt(Integer::intValue)
                .sum();

        return new AdmissionCycleDetailResponse(
                cycle.getId(),
                cycle.getAcademicYear(),
                cycle.getName(),
                cycle.getStatus(),
                cycle.getInquiryOpenAt(),
                cycle.getApplicationOpenAt(),
                cycle.getApplicationCloseAt(),
                cycle.getEnrollmentDeadlineAt(),
                rows,
                rows.size(),
                total,
                cycle.getNotes(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt());
    }

    /**
     * Endpoint #2 — corrects a cycle's name, dates or notes.
     *
     * <p><b>Only what was sent moves.</b> An absent field is left alone; a field named in
     * is emptied by {@code ""}. Sending nothing that moves is {@code NOTHING_TO_UPDATE} rather than a silent
     * success, because a no-op that answers 200 looks exactly like a change that worked.
     *
     * <p><b>The dates are checked as they will END UP, not as they were sent.</b> That is the
     * whole difficulty of this endpoint: moving the close date earlier than a stored open date is
     * only wrong once the two are put together, and checking the request alone would let it
     * through.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND  no cycle with that id in this school
     * 400 NOTHING_TO_UPDATE          the body moves nothing
     * 400 BLANK_CYCLE_NAME           name sent as "" — a cycle needs one
     * 409 CYCLE_NAME_TAKEN           the new name is already used in that year
     * 400 CYCLE_DATES_OUT_OF_ORDER   the result would not run forwards
     * 409 CONCURRENT_MODIFICATION    a version was sent and the cycle has moved on
     * </pre>
     */
    public AdmissionCycleResponse updateCycle(String admissionCycleId,
            AdmissionCycleUpdateRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String id = admissionCycleId == null ? "" : admissionCycleId.trim();
        log.info("[updateCycle] Step 1: Reading cycle {} to correct it", id);

        //! step 2 - the cycle, scoped by school in the query for the same reason #6 is.
        AdmissionCycle cycle = helper.loadCycle(school, id);

        //! step 3 - has somebody else changed it since the caller looked?
        //! Optional: a correction decided from a screen that might be stale sends the version it
        //! saw, and gets a refusal instead of writing over somebody's work. Leaving it out is
        //! last-write-wins, which is the right default for a document one person edits at a time.
        if (request.version() != null && !request.version().equals(cycle.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This cycle has changed since you read it — it is now version "
                            + cycle.getVersion() + " and you sent " + request.version()
                            + ". Read it again and redo the correction.");
        }

        //! step 5 - the merged cycle, field by field. Nothing is written yet: this works out what
        //! the cycle WOULD look like, so the checks below can be run against the result rather
        //! than against the request.
        boolean moved = false;

        //! 5a - the name. Cannot be emptied: it is the only thing telling two rounds of one year
        //! apart, so "" is a refusal rather than a clear.
        String name = cycle.getName();
        if (request.name() != null) {
            String sent = request.name().trim();
            if (sent.isEmpty()) {
                throw ApiException.badRequest("BLANK_CYCLE_NAME",
                        "A cycle needs a name — it is what tells two rounds of the same year "
                                + "apart. Send a name, or leave the field out to keep this one.");
            }
            if (!sent.equals(name)) {
                name = sent;
                moved = true;
            }
        }

        //! 5b - the four dates. Moveable, and there is no way to empty one: they are required on
        //! create, so a correction that could blank one would leave a cycle #1 would not have made.
        Map<String, Instant> sentDates = new LinkedHashMap<>();
        sentDates.put("inquiryOpenAt", request.inquiryOpenAt());
        sentDates.put("applicationOpenAt", request.applicationOpenAt());
        sentDates.put("applicationCloseAt", request.applicationCloseAt());
        sentDates.put("enrollmentDeadlineAt", request.enrollmentDeadlineAt());

        Map<String, Instant> storedDates = new LinkedHashMap<>();
        storedDates.put("inquiryOpenAt", cycle.getInquiryOpenAt());
        storedDates.put("applicationOpenAt", cycle.getApplicationOpenAt());
        storedDates.put("applicationCloseAt", cycle.getApplicationCloseAt());
        storedDates.put("enrollmentDeadlineAt", cycle.getEnrollmentDeadlineAt());

        Map<String, Instant> merged = new LinkedHashMap<>(storedDates);
        for (String field : AdmissionCycleServiceUtils.DATE_FIELDS) {
            Instant sent = sentDates.get(field);
            if (sent != null && !sent.equals(storedDates.get(field))) {
                merged.put(field, sent);
                moved = true;
            }
        }

        //! 5c - the notes. "" EMPTIES THEM, which is the project's one convention — #9 and #18
        //! use it too. This endpoint also took a `clear` list naming the field until 2026-09-25;
        //! it was removed because "" already did the job and two spellings for one action is two
        //! things to document, two to test, and a refusal for callers who sent both.
        String notes = cycle.getNotes();
        boolean clearNotes = request.notes() != null && request.notes().trim().isEmpty();
        if (clearNotes) {
            if (notes != null) {
                moved = true;
            }
            notes = null;
        } else if (request.notes() != null) {
            String sent = request.notes().trim();
            if (!sent.equals(notes)) {
                notes = sent;
                moved = true;
            }
        }

        //! step 6 - nothing moved. A 200 here would look exactly like a correction that worked,
        //! and the caller would have no way to tell that their change went nowhere.
        if (!moved) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Nothing in that request changes this cycle. Send a different name, a date, "
                            + "or notes.");
        }
        log.info("[updateCycle] Step 2: The request changes something, checking it is allowed");

        //! step 7 - the name has to stay free inside the year. Only asked when the name actually
        //! moved: a request that sends the current name back is not a clash with itself, and
        //! existsBy would say it is.
        if (!name.equals(cycle.getName())) {
            // TODO: check admission cycle exists
            if (admissionCycles.existsBySchoolIdAndAcademicYearAndName(
                    school.getId(), cycle.getAcademicYear(), name)) {
                throw ApiException.conflict("CYCLE_NAME_TAKEN",
                        "'" + cycle.getAcademicYear() + "' already has an admission cycle called '"
                                + name + "'.");
            }
        }

        //! step 8 - the dates have to run forwards AS THEY WILL END UP. This is the check that
        //! makes the endpoint harder than it looks: sending only a close date is fine on its own
        //! and wrong against the open date already stored, and only the merged four can tell.
        SchoolTimeZone zone = schoolZone.of(school);

        //! step 8a - INSIDE THE CYCLE'S YEAR, checked BEFORE the order, for the reason #1 records:
        //! a middle date outside the year is out of order too, and the order check would take the
        //! blame for a problem that is really about the year.
        //!
        //! CHECKED ON THE MERGE, not on what was sent: moving one date can take it outside the
        //! year while the other three stay put, and only the merged set can tell.
        //!
        //! #2 CANNOT CHANGE THE YEAR, so the cycle's own is the one to check against. If it ever
        //! can, this line is what has to start using the new one.
        Map<String, Instant> inYear = new LinkedHashMap<>();
        for (String field : AdmissionCycleServiceUtils.DATE_FIELDS) {
            inYear.put(field, merged.get(field));
        }
        yearWindow.requireInside(school, cycle.getAcademicYear(), zone,
                "CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR", inYear);

        //! step 8b - and then the merged four have to run forwards.
        Instant earlier = null;
        String earlierName = null;
        for (int i = 0; i < AdmissionCycleServiceUtils.DATE_FIELDS.size(); i++) {
            Instant when = merged.get(AdmissionCycleServiceUtils.DATE_FIELDS.get(i));
            if (when == null) {
                continue;
            }
            if (earlier != null && when.isBefore(earlier)) {
                throw ApiException.badRequest("CYCLE_DATES_OUT_OF_ORDER",
                        "That would leave the dates in the wrong order: " + DATE_NAMES.get(i)
                                + " is " + Dates.readable(when, zone) + ", which is before "
                                + earlierName + " at " + Dates.readable(earlier, zone) + ".");
            }
            earlier = when;
            earlierName = DATE_NAMES.get(i);
        }

        //! step 9 - put the new values on the object. Built first, saved next, so the values can
        //! be seen before they are written.
        cycle.setName(name);
        cycle.setInquiryOpenAt(merged.get("inquiryOpenAt"));
        cycle.setApplicationOpenAt(merged.get("applicationOpenAt"));
        cycle.setApplicationCloseAt(merged.get("applicationCloseAt"));
        cycle.setEnrollmentDeadlineAt(merged.get("enrollmentDeadlineAt"));
        cycle.setNotes(notes);

        //! step 10 - save. An update, not an insert: the object was read from the database first.
        //! Spring Data checks @Version here, so a racing writer is a DataIntegrityViolation that
        //! the global handler answers as 409 CONCURRENT_MODIFICATION.
        // TODO: update admission cycle
        AdmissionCycle saved = admissionCycles.save(cycle);
        log.info("[updateCycle] Step 3: Saved cycle {} as version {}",
                saved.getId(), saved.getVersion());

        return AdmissionCycleResponse.fromCycle(saved,
                "'" + saved.getName() + "' was corrected. Setting the seats is #4 and opening the "
                        + "cycle is #3, neither of which is built. " + AdmissionCycleServiceUtils.NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #4 — sets the cycle's seat table, whole.
     *
     * <p><b>This REPLACES.</b> A shorter list removes the rows left out; an empty list clears the
     * table. Whoever sets intake reads the whole thing and rewrites it, and an embedded row has no
     * id to address on its own.
     *
     * <p><b>Every class must belong to the cycle's own academic year</b>, not merely to the
     * school. A cycle admits into one year, so seats against another year's class would be seats
     * nobody could ever fill.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND  no cycle with that id in this school
     * 409 DUPLICATE_CAPACITY_CLASS   one class listed twice
     * 409 CLASS_NOT_IN_CYCLE_YEAR    a class that is not in the cycle's year
     * 400 RESERVED_EXCEEDS_TOTAL     reservedSeats above totalSeats
     * 409 CONCURRENT_MODIFICATION    a version was sent and the cycle has moved on
     * </pre>
     */
    public AdmissionCycleDetailResponse setCapacities(String admissionCycleId,
            AdmissionCycleCapacitiesRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String id = admissionCycleId == null ? "" : admissionCycleId.trim();
        log.info("[setCapacities] Step 1: Reading cycle {} to set its seats", id);

        //! step 2 - the cycle, scoped by school in the query.
        AdmissionCycle cycle = helper.loadCycle(school, id);

        //! step 3 - has somebody else changed it since the caller read the table?
        //! MATTERS MORE HERE THAN ON #2, because this write replaces: two people setting intake
        //! from two stale screens means one of them silently loses every row the other added.
        if (request.version() != null && !request.version().equals(cycle.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This cycle has changed since you read it — it is now version "
                            + cycle.getVersion() + " and you sent " + request.version()
                            + ". Read the seat table again and redo your changes.");
        }

        List<AdmissionCycleCapacitiesRequest.Seat> sent = request.capacities();
        log.info("[setCapacities] Step 2: Checking the {} row(s) that were sent", sent.size());

        //! step 4 - one row per class. Two rows for one class is a request with two answers, and
        //! picking either would be a guess; the stored table has no row identity to merge them by.
        Set<String> seen = new LinkedHashSet<>();
        for (AdmissionCycleCapacitiesRequest.Seat seat : sent) {
            String classId = seat.classDocsId().trim();
            if (!seen.add(classId)) {
                throw ApiException.conflict("DUPLICATE_CAPACITY_CLASS",
                        "Class '" + classId + "' is listed twice. Each class gets one row — set "
                                + "the seats you want on a single one.");
            }
        }

        //! step 5 - reserved cannot exceed total. Checked per row so the message can say which.
        //! Bean validation cannot do this one: it compares two fields of the same row.
        for (AdmissionCycleCapacitiesRequest.Seat seat : sent) {
            int reserved = seat.reservedSeats() == null ? 0 : seat.reservedSeats();
            if (reserved > seat.totalSeats()) {
                throw ApiException.badRequest("RESERVED_EXCEEDS_TOTAL",
                        "Class '" + seat.classDocsId().trim() + "' reserves " + reserved
                                + " of " + seat.totalSeats() + " seats. A class cannot hold back "
                                + "more seats than it is offering.");
            }
        }

        //! step 6 - every class has to be one of THIS CYCLE'S YEAR. One query for all of them,
        //! never one per row: a table of twenty classes is twenty round trips otherwise.
        //!
        //! The year is the cycle's, not the school's current one. A cycle admits into one year and
        //! seats against another year's class are seats nobody could fill.
        if (!seen.isEmpty()) {
            //! THE KEYS ARE THE IDS THAT EXIST — a class that is gone is simply absent from the
            //! map, which is the same fact #6 uses to render a seat row with no name.
            Set<String> known = utils.classNamesFor(school, cycle.getAcademicYear(), seen).keySet();

            for (String classId : seen) {
                if (!known.contains(classId)) {
                    throw ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                            "Class '" + classId + "' is not a class of '"
                                    + cycle.getAcademicYear() + "', which is the year this cycle "
                                    + "admits into. Seats against another year's class could "
                                    + "never be filled.");
                }
            }
        }

        //! step 7 - build the rows. reservedSeats defaults to 0 rather than staying null, so a
        //! reader never has to decide what an absent reservation means.
        //!
        //! NOT COVERED BY ANY TEST, AND MEASURED TO BE UNREACHABLE. Mutation C6 removed this
        //! guard and the response was byte-identical, because IntakeCapacity already carries
        //! `@Builder.Default private Integer reservedSeats = 0`. The model does the work. This
        //! stays because it says what the endpoint promises at the place the promise is made,
        //! and because a default two files away is one refactor from disappearing quietly.
        List<IntakeCapacity> rows = new ArrayList<>();
        for (AdmissionCycleCapacitiesRequest.Seat seat : sent) {
            IntakeCapacity row = IntakeCapacity.builder()
                    .classDocsId(seat.classDocsId().trim())
                    .totalSeats(seat.totalSeats())
                    .reservedSeats(seat.reservedSeats() == null ? 0 : seat.reservedSeats())
                    .build();
            rows.add(row);
        }

        //! step 8 - REPLACE the table. Not a merge: the caller sent what the table should be, and
        //! anything they left out is a row they removed.
        cycle.setCapacities(rows);

        //! step 9 - save. An update, not an insert: the cycle was read from the database first.
        // TODO: update admission cycle
        AdmissionCycle saved = admissionCycles.save(cycle);
        log.info("[setCapacities] Step 3: Saved {} seat row(s) on cycle {}",
                rows.size(), saved.getId());

        //! step 10 - hand back the FULL cycle, the same shape #6 returns, so a caller that just
        //! set the table sees it back with the class names resolved rather than having to ask
        //! again.
        //!
        //! THIS COSTS ONE EXTRA READ of the document we just saved. Worth it: the alternative is a
        //! second copy of #6's name resolution, and two copies of that is two places for the
        //! "a class that is gone keeps its row" rule to drift apart.
        return getCycle(saved.getId());
    }

    /**
     * Endpoint #3 — moves a cycle through its lifecycle.
     *
     * <p><b>This is what the rest of the module waits for.</b> An application can only be
     * submitted into a cycle that is {@code OPEN} ({@code CYCLE_NOT_OPEN}, this module's
     * replacement for gate 4), and until this endpoint existed no cycle could leave {@code DRAFT}.
     *
     * <p><b>Opening needs a seat table.</b> [#17] refuses an application whose class is not in the
     * cycle's capacities — {@code CLASS_NOT_IN_CAPACITY} — so a cycle opened with an empty table
     * is a funnel nothing can enter. Refusing here is the difference between a mistake caught now
     * and a round nobody can apply to.
     *
     * <pre>
     * 404 ADMISSION_CYCLE_NOT_FOUND  no cycle with that id in this school
     * 409 INVALID_CYCLE_TRANSITION   the graph does not have that move
     * 409 CYCLE_HAS_NO_SEATS         opening a cycle whose seat table is empty
     * 409 CONCURRENT_MODIFICATION    a version was sent and the cycle has moved on
     * </pre>
     */
    public AdmissionCycleResponse moveStatus(String admissionCycleId,
            AdmissionCycleStatusRequest request) {

        //! step 1 - who is asking. requireUsable, because this is a write.
        School school = currentSchool.requireUsable();
        String id = admissionCycleId == null ? "" : admissionCycleId.trim();
        log.info("[moveStatus] Step 1: Reading cycle {} to move its status", id);

        //! step 2 - the cycle, scoped by school in the query.
        AdmissionCycle cycle = helper.loadCycle(school, id);

        //! step 3 - has somebody else moved it since the caller looked?
        if (request.version() != null && !request.version().equals(cycle.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "This cycle has changed since you read it — it is now version "
                            + cycle.getVersion() + " and you sent " + request.version()
                            + ". Read it again before moving it.");
        }

        AdmissionCycleStatus from = cycle.getStatus();
        AdmissionCycleStatus to = request.status();
        log.info("[moveStatus] Step 2: Checking {} -> {} is a move the graph has", from, to);

        //! step 4 - is it already there? Not a refusal worth its own code, but not a silent
        //! success either: a caller who thinks they opened a cycle that was already open should
        //! be told nothing happened.
        if (from == to) {
            throw ApiException.conflict("INVALID_CYCLE_TRANSITION",
                    "This cycle is already " + from + ".");
        }

        //! step 5 - does the graph have this move? The table is the specification; the model
        //! README's diagram describes it and cannot enforce it.
        Set<AdmissionCycleStatus> allowed =
                CYCLE_MOVES.getOrDefault(from, EnumSet.noneOf(AdmissionCycleStatus.class));
        if (!allowed.contains(to)) {
            String reachable = allowed.isEmpty()
                    ? "nothing — " + from + " is where a cycle stops"
                    : allowed.stream().map(Enum::name).collect(Collectors.joining(", "));
            throw ApiException.conflict("INVALID_CYCLE_TRANSITION",
                    "A cycle cannot go from " + from + " to " + to + ". From " + from
                            + " it can reach: " + reachable + ".");
        }

        //! step 6 - opening needs somewhere for applicants to go.
        //! #17 refuses an application whose class is not in this table, so opening with an empty
        //! one builds a round nobody can apply to. Checked only on the way IN to OPEN: a cycle
        //! already open whose table was emptied afterwards is a different problem, and closing or
        //! cancelling it must never be blocked.
        if (to == AdmissionCycleStatus.OPEN
                && (cycle.getCapacities() == null || cycle.getCapacities().isEmpty())) {
            throw ApiException.conflict("CYCLE_HAS_NO_SEATS",
                    "'" + cycle.getName() + "' has no seats set up, so nothing could be applied "
                            + "for. Set the seat table first — an application names a class, and "
                            + "a class that is not in the table is refused.");
        }

        //! step 7 - record WHEN it happened, where the school published nothing.
        //!
        //! FILLS AN ABSENT DATE, NEVER OVERWRITES A SET ONE. The four dates are the school's
        //! published calendar - what families were told - and a round opened two days early must
        //! not rewrite the date on the prospectus. Where the school published nothing, the moment
        //! the button was pressed is the best record there is.
        //!
        //! ONE FIELD, TWO FACTS. The plan and the actual both want to live here and only one can.
        //! When a date is already set, the plan wins and the actual is not recorded anywhere -
        //! an `actualOpenedAt` on the model is what would fix that.
        Instant happenedAt = Instant.now();
        String dateNote = "";
        String dateField = switch (to) {
            case SCHEDULED -> "inquiryOpenAt";
            case OPEN -> "applicationOpenAt";
            case CLOSED -> "applicationCloseAt";
            case COMPLETED -> "enrollmentDeadlineAt";
            //! CANCELLED gets none. None of the four means "abandoned", and writing the moment
            //! into one of them would claim something the field does not say.
            case CANCELLED, DRAFT -> null;
        };

        if (dateField != null) {
            Map<String, Instant> after = new LinkedHashMap<>();
            after.put("inquiryOpenAt", cycle.getInquiryOpenAt());
            after.put("applicationOpenAt", cycle.getApplicationOpenAt());
            after.put("applicationCloseAt", cycle.getApplicationCloseAt());
            after.put("enrollmentDeadlineAt", cycle.getEnrollmentDeadlineAt());

            if (after.get(dateField) != null) {
                dateNote = " The published " + dateField + " was left as it was.";
            } else {
                after.put(dateField, happenedAt);

                //! WOULD FILLING IT CONTRADICT THE REST? A round opened late but planned to close
                //! early would end up closing before it opened. Better to record nothing than to
                //! record an order that cannot be true - and NEVER to refuse the move, because a
                //! cycle trapped by its own calendar is worse than a missing timestamp.
                if (utils.datesRunForwards(after)) {
                    switch (dateField) {
                        case "inquiryOpenAt" -> cycle.setInquiryOpenAt(happenedAt);
                        case "applicationOpenAt" -> cycle.setApplicationOpenAt(happenedAt);
                        case "applicationCloseAt" -> cycle.setApplicationCloseAt(happenedAt);
                        default -> cycle.setEnrollmentDeadlineAt(happenedAt);
                    }
                    dateNote = " " + dateField + " was recorded as now, because the school had "
                            + "published none.";
                } else {
                    dateNote = " " + dateField + " was left empty: filling it with now would put "
                            + "the cycle's dates out of order.";
                }
            }
        }

        //! step 8 - move it. Built, then saved, so the new values are visible before they go.
        cycle.setStatus(to);

        //! step 9 - save
        // TODO: update admission cycle
        AdmissionCycle saved = admissionCycles.save(cycle);
        log.info("[moveStatus] Step 3: Moved cycle {} from {} to {}.{}",
                saved.getId(), from, to, dateNote);

        return AdmissionCycleResponse.fromCycle(saved, utils.nextStepFor(saved, from) + dateNote);
    }

    /**
     * Endpoint #7 — <b>seats against reality</b>.
     *
     * <p><b>The counterpart to a decision #29 made on purpose.</b> #29 does not cap offers against
     * the seat table, because schools deliberately over-offer — sixty letters for forty places,
     * because a fifth of families go elsewhere. The note written there was "counting offers against
     * places is #7's job", and until this existed <b>over-offering was invisible</b>: nothing
     * anywhere told a school it had promised more seats than it has.
     *
     * <p><b>{@code freeSeats} is allowed to go negative, and that is the whole endpoint.</b>
     * Clamping it at zero would hide the one thing it is for — and "0 free" cannot tell "exactly
     * full" from "twenty over".
     *
     * <p><b>One grouped aggregation for the whole table</b>, not one query per class. A cycle with
     * twenty classes is one round trip, and the counts are computed rather than stored so that
     * {@code AdmissionCycle} does not become a document every application write has to touch.
     *
     * <p><b>A KNOWN SKEW, written down rather than hidden.</b> The counts group applications by
     * {@code appliedClassDocsId} — what the family asked for. A school may offer a <i>different</i>
     * grade (#29 allows it deliberately), and that seat is then counted against the class applied
     * for rather than the class promised. Fixing it means a second aggregation over
     * {@code admission_offers} keyed on {@code offeredClassDocsId}, which the plan's own collection
     * list for this endpoint predates. It is the minority case and it is not silent: this note is
     * the record of it.
     *
     * <p><b>No gates.</b> A read — a suspended school still needs to know what it promised.
     */
    public AdmissionCycleCapacityResponse getCapacity(String admissionCycleId) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        log.info("[getCapacity] Step 1: Counting seats for cycle {} in school {}",
                admissionCycleId, school.getId());

        //! step 2 - the round, scoped by school in the QUERY. It THROWS: a capacity report about a
        //! round nobody can find is not a report.
        AdmissionCycle cycle = helper.loadCycle(school, admissionCycleId);

        //! step 3 - the counts. ONE AGGREGATION for every class and every status at once.
        // TODO: read admission applications (grouped counts)
        List<ClassStatusCount> counted = applications
                .countByClassAndStatus(school.getId(), cycle.getId());

        Map<String, Map<AdmissionApplicationStatus, Long>> byClass = new HashMap<>();
        for (ClassStatusCount one : counted) {
            byClass.computeIfAbsent(one.classDocsId() == null ? "" : one.classDocsId(),
                            key -> new EnumMap<>(AdmissionApplicationStatus.class))
                    .merge(one.status(), one.count(), Long::sum);
        }

        //! step 4 - the class names, for the rows. The same one-query-for-the-table read #6 makes.
        List<IntakeCapacity> seats = cycle.getCapacities() == null
                ? List.of()
                : cycle.getCapacities();

        Map<String, String> classNames = utils.classNamesFor(school, cycle.getAcademicYear(),
                seats.stream().map(IntakeCapacity::getClassDocsId).toList());

        //! step 5 - one row per CONFIGURED class, and only those. A class nobody set seats for is
        //! not part of this round: #17 refuses an application for it, so it cannot have applicants.
        List<AdmissionCycleCapacityResponse.Row> rows = seats.stream()
                .map(seat -> utils.rowFor(seat, classNames.get(seat.getClassDocsId()),
                        byClass.getOrDefault(seat.getClassDocsId(), Map.of())))
                .toList();

        int over = (int) rows.stream()
                .filter(AdmissionCycleCapacityResponse.Row::overCommitted)
                .count();

        log.info("[getCapacity] Step 2: {} class(es), {} over-committed", rows.size(), over);

        return new AdmissionCycleCapacityResponse(
                cycle.getId(), cycle.getName(), cycle.getAcademicYear(),
                cycle.getStatus() == null ? null : cycle.getStatus().name(),
                rows, utils.totalOf(rows), over,
                utils.capacityNextStep(cycle, rows, over) + " " + AdmissionCycleServiceUtils.NO_AUTHORIZATION_YET);
    }

}
