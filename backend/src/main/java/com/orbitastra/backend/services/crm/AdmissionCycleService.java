package com.orbitastra.backend.services.crm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleCreateRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.request.AdmissionCycleSearchRequest;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleDetailResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleResponse;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleSummaryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.crm.admissioncycle.AdmissionCycleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Admission cycles — one round of admissions for one academic year. Endpoints #1, #5 and #6 of
 * the plan in {@code controllers/crm/README.md}; only those three are built.
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
@RequiredArgsConstructor
public class AdmissionCycleService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionCycleService.class);

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    private final AdmissionCycleRepository admissionCycles;
    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolZone schoolZone;

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
                        + "it is OPEN. " + NO_AUTHORIZATION_YET);
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
        // TODO: read admission cycle
        AdmissionCycle cycle = admissionCycles.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("ADMISSION_CYCLE_NOT_FOUND",
                        "No admission cycle with id '" + id + "' in this school."));

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

        //! NOTHING TO LOOK UP IS NOT A QUERY. Every DRAFT cycle has an empty seat table until #4
        //! is built, so this is the common case rather than an edge one.
        // TODO: read school classes
        List<SchoolClass> classes = classIds.isEmpty()
                ? List.of()
                : schoolClasses.findBySchoolIdAndAcademicYearAndIdIn(
                        school.getId(), cycle.getAcademicYear(), classIds);

        //! ASSIGNED ONCE, because the lambda below captures it. A merge function is needed even
        //! though ids are unique: toMap throws on a duplicate key rather than keeping either.
        Map<String, String> classNames = classes.stream().collect(Collectors.toMap(
                SchoolClass::getId, SchoolClass::getName, (first, second) -> first));
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
}
