package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableCreateRequest;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableSearchRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableResponse;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableSummaryResponse;
import com.orbitastra.backend.dto.academics.timetable.response.SkippedDateResponse;
import com.orbitastra.backend.dto.academics.timetable.response.TimetableCreateResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.timetable.DailyTimetable;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.timetable.DailyTimetableRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.academics.helper.TimetableHelper;
import com.orbitastra.backend.services.academics.utils.DailyTimetableServiceUtils;

import lombok.RequiredArgsConstructor;

/**
 * Where every child is meant to be, hour by hour — the endpoints in
 * {@code controllers/academics/timetable/README.md}. #1 is built.
 *
 * <p><b>All three gates run in the controller</b>, as everywhere else in {@code academics}. That
 * is true because the academic year is named in the URL: until 2026-09-17 it was derived from a
 * date in the body, a range could span two years, and gate 4 had nothing to ask about before the
 * request had been read — so the check lived here as a refusal. Stating the year removed both the
 * deviation and the per-date year lookup that went with it.
 */
@Service
@RequiredArgsConstructor
public class DailyTimetableService {

    private final DailyTimetableRepository timetables;
    private final StaffRepository staff;
    private final DailyTimetableServiceUtils utils;
    private final TimetableHelper helper;
    private final CurrentSchoolResolver currentSchool;

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /** How many days one request may cover. A term is about 90 working days. */
    private static final int MAX_RANGE_DAYS = 120;

    /**
     * What {@code ?sort=} accepts on #10, keyed by the lower-cased name a caller types.
     *
     * <p>An allowlist rather than a pass-through, for the reason every list in this project has
     * one: an arbitrary field name reaching a Mongo sort is how a caller orders by something
     * unindexed and makes the database read every document to answer one page.
     *
     * <p><b>None of the counts is sortable.</b> They are not on the document — the aggregation
     * computes them <i>after</i> the page has been chosen, so ordering by one would mean counting
     * the whole year first. "The busiest day" is a different endpoint, not a sort.
     */
    private static final Map<String, String> SORTABLE_DAY_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_DAY_FIELDS.put("date", "date");
        SORTABLE_DAY_FIELDS.put("createdat", "createdAt");
        SORTABLE_DAY_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set as a sentence, for the refusal to list. */
    private static final String SORTABLE_DAY_FIELD_NAMES =
            SORTABLE_DAY_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order, and the tiebreaker on every other sort.
     *
     * <p><b>Ascending, and one field is enough.</b> A school reads a week forwards, and
     * {@code school_timetable_date_uniq} makes the date unique per school — so it is already a
     * total order and needs no tiebreaker. That is unusual here: most lists in this project need
     * two fields because their first choice can tie.
     */
    private static final Sort DAY_ORDER = Sort.by(Sort.Order.asc("date"));

    //! endpoint 1 — create a day, or a range of them ----------------------------------

    /**
     * Endpoint #1 — write one day's periods across one date or a range of them.
     *
     * <h2>The range is the point</h2>
     *
     * <p>A school does not build one Tuesday; it builds a pattern and applies it. {@code startDate}
     * alone writes one day; {@code endDate} too writes every date between them inclusive, each
     * carrying the same periods.
     *
     * <h2>Holidays are skipped, collisions refuse everything</h2>
     *
     * <p>Those two look inconsistent and are not. A holiday inside a range is <b>expected</b> —
     * any range longer than about five days contains a weekly off — so refusing the whole request
     * over one would make ranges useless, and the skipped dates are named in the response instead.
     * A date that <b>already has a timetable</b> is not expected: it means the caller is rebuilding
     * something, and writing half a range would leave a school unable to tell which days came from
     * which request. All of it or none of it, with every colliding date named.
     *
     * <p>A range in which <i>every</i> date is a holiday writes nothing, and that is a refusal —
     * there is no partial success to report.
     *
     * <h2>Validated once, written many times</h2>
     *
     * <p>The periods are identical for every date, so the shape rules — times, slot fields, period
     * codes, and the three overlap checks — are run <b>once</b> against the built entries rather
     * than once per date. What is genuinely per date is the academic year, the holiday check, and
     * the structure lookups, because a range can cross a year boundary and a class belongs to one
     * year.
     *
     * <h2>Entry ids are generated here</h2>
     *
     * <p>MongoDB does not generate {@code _id} for embedded documents — rule 1 of the persistence
     * contract — and <b>each date gets its own ids</b>: they are different periods on different
     * days, and sharing an id would make {@code AttendanceSession.timetableEntryId} ambiguous
     * across dates.
     */
    @Transactional
    public TimetableCreateResponse createTimetables(String academicYear,
            DailyTimetableCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the year this timetable belongs to, STATED rather than worked out from the
        //! dates. Read again here because the controller's gate 4 does not hand the document down;
        //! one extra read, against the alternative of a controller passing domain objects into a
        //! service, which nothing else in this project does.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 3 - the range. An absent endDate means one day.
        LocalDate start = request.startDate();
        LocalDate end = request.resolvedEndDate();

        if (end.isBefore(start)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "endDate " + end + " is before startDate " + start + ".");
        }

        long days = start.datesUntil(end.plusDays(1)).count();
        if (days > MAX_RANGE_DAYS) {
            throw ApiException.badRequest("DATE_RANGE_TOO_LONG",
                    "This range covers " + days + " days. One request may cover at most "
                            + MAX_RANGE_DAYS + " — about a term of working days. Split it.");
        }

        //! step 4 - THE CHECK THE DERIVATION COULD NOT MAKE. Every date has to fall inside the
        //! year the caller named. Working the year out from the date meant every date resolved to
        //! SOMETHING, so "you asked for the wrong year" was not a sentence this endpoint could
        //! say; it wrote into whichever year happened to contain the date instead.
        //!
        //! Checked on the range's ENDS rather than every date, because a range is contiguous: if
        //! both ends are inside the year, everything between them is too.
        if (start.isBefore(year.getStartDate()) || end.isAfter(year.getEndDate())) {
            throw ApiException.conflict("DATE_OUTSIDE_ACADEMIC_YEAR",
                    "'" + year.getName() + "' runs from " + year.getStartDate() + " to "
                            + year.getEndDate() + ", so " + start
                            + (start.equals(end) ? "" : " to " + end)
                            + " is outside it. Pick dates inside the year, or name the year those "
                            + "dates belong to.");
        }

        List<LocalDate> dates = start.datesUntil(end.plusDays(1)).toList();

        //! step 5 - no date in the range may already have a timetable. ONE query for the whole
        //! range; fourteen existsBy... calls is the N+1 that makes a longer range slower for no
        //! reason a caller can see.
        // TODO: read daily timetables
        List<DailyTimetable> existing = timetables.findBySchoolIdAndDateIn(school.getId(), dates);

        if (!existing.isEmpty()) {
            List<String> taken = existing.stream()
                    .map(one -> String.valueOf(one.getDate()))
                    .sorted()
                    .toList();

            throw ApiException.conflict("TIMETABLE_ALREADY_EXISTS",
                    "A timetable already exists for " + String.join(", ", taken)
                            + ". Nothing was written: a range is created whole or not at all, so "
                            + "that a school can always tell which days came from which request.");
        }

        //! step 6 - build the entries ONCE, before any of the shape checks, so that what is
        //! validated is what will be stored rather than what was sent. The same order #1 of
        //! grading settled on for bands.
        //!
        //! The ids here are placeholders: each date gets its own set in step 12, because two dates
        //! sharing an entry id would make AttendanceSession.timetableEntryId ambiguous.
        List<TimetableEntry> shape = request.entries().stream()
                .map(utils::toEntry)
                .toList();

        //! step 7 - the structure every period has to fit: the class, the section, and a subject
        //! that section actually studies.
        //!
        //! ONCE, not once per date. The periods are identical for every date and a class belongs
        //! to one academic year, so the answer cannot differ between dates of the same year — and
        //! a range IS one year, because step 4 refuses anything outside it.
        //!
        //! One read per CLASS, not one per period: a day of four hundred periods across twelve
        //! classes is twelve queries.
        //!
        //! IT RUNS BEFORE THE OVERLAP CHECKS, and that order is load-bearing since 2026-09-17.
        //! The section is resolved against the class case-insensitively, so "A" and "a" name one
        //! section - but the overlap check compared the two strings, saw two sections, and let
        //! 10-A be given two periods at 09:30. Normalising here means everything below compares
        //! the class's own spelling, and the day is stored with one spelling per section.
        Map<String, SchoolClass> classes = new LinkedHashMap<>();

        for (TimetableEntry entry : shape) {
            SchoolClass schoolClass = classes.get(entry.getClassDocsId());
            if (schoolClass == null) {
                schoolClass = utils.loadClassForYear(school, year.getName(),
                        entry.getClassDocsId());
                classes.put(entry.getClassDocsId(), schoolClass);
            }

            //! THE CLASS'S OWN SPELLING replaces whatever was sent. A section is its sectionNo -
            //! what an AttendanceSession stores and what every later read looks up by - so two
            //! spellings of one section in a day means a lookup by either finds half the periods.
            entry.setSectionNo(helper.requireActiveSection(schoolClass, entry.getSectionNo()));

            helper.validateSubjectForSection(schoolClass, entry.getSubjectCode(),
                    entry.getSectionNo());
        }

        //! step 8 - the rules that depend only on the periods themselves, run once for the whole
        //! range because every date carries the same set. Order matters: an inverted period
        //! checked for overlap reports a clash, which is true and names the wrong problem.
        helper.validateTimes(shape);
        helper.validateSlotFields(shape);
        helper.validatePeriodCodesUnique(shape);
        helper.validateNoSectionOverlap(shape);
        helper.validateNoTeacherOverlap(shape);
        helper.validateNoRoomOverlap(shape);

        //! step 9 - every teacher named has to be this school's. Read in ONE query rather than one
        //! per period; a day of four hundred periods would otherwise be four hundred round trips.
        Set<String> teacherIds = new LinkedHashSet<>();
        for (TimetableEntry entry : shape) {
            if (entry.getTeacherDocsId() != null) {
                teacherIds.add(entry.getTeacherDocsId());
            }
        }

        if (!teacherIds.isEmpty()) {
            // TODO: read staff
            Set<String> found = new LinkedHashSet<>();
            for (Staff person : staff.findBySchoolIdAndIdIn(school.getId(), teacherIds)) {
                found.add(person.getId());
            }
            for (String wanted : teacherIds) {
                if (!found.contains(wanted)) {
                    throw ApiException.notFound("TEACHER_NOT_FOUND",
                            "No staff member with id '" + wanted + "' in this school.");
                }
            }
        }

        //! step 10 - which dates are working days. One year now, so there is no per-date year
        //! lookup and no running check here: gate 4 asked that once, in the controller.
        List<LocalDate> workingDates = new ArrayList<>();
        List<SkippedDateResponse> skipped = new ArrayList<>();

        for (LocalDate date : dates) {
            //! A HOLIDAY IS SKIPPED, NOT REFUSED - see the class note. Nothing about the day of
            //! the week is consulted: a school that runs on Sunday is a normal school.
            String holiday = utils.holidayNameFor(year, date);
            if (holiday != null) {
                skipped.add(new SkippedDateResponse(date, "NOT_A_WORKING_DAY", holiday));
                continue;
            }
            workingDates.add(date);
        }

        //! step 11 - nothing to write means every date was a holiday. A refusal rather than an
        //! empty success: the caller asked for a timetable and has none, and a 201 saying "0
        //! created" reads as though something worked.
        if (workingDates.isEmpty()) {
            throw ApiException.conflict("NOT_A_WORKING_DAY",
                    "Every date from " + start + " to " + end + " is a holiday or weekly off for "
                            + "this school, so no timetable was written.");
        }

        //! step 12 - build a document per working date. Fresh entry ids for each: they are
        //! different periods on different days, and two dates sharing an id would make
        //! AttendanceSession.timetableEntryId ambiguous.
        //!
        //! schoolId explicitly, like every write in this project - nothing validates a document on
        //! save, and one written without it is invisible to every tenant-scoped query afterwards.
        List<DailyTimetable> toInsert = new ArrayList<>();

        for (LocalDate date : workingDates) {
            List<TimetableEntry> entries = new ArrayList<>(shape.size());
            for (TimetableEntry one : shape) {
                entries.add(TimetableEntry.builder()
                        .id(new ObjectId().toHexString())
                        .periodCode(one.getPeriodCode())
                        .classDocsId(one.getClassDocsId())
                        .sectionNo(one.getSectionNo())
                        .slotType(one.getSlotType())
                        .subjectCode(one.getSubjectCode())
                        .teacherDocsId(one.getTeacherDocsId())
                        .slotLabel(one.getSlotLabel())
                        .startTime(one.getStartTime())
                        .endTime(one.getEndTime())
                        .facilityResourceDocsId(one.getFacilityResourceDocsId())
                        .build());
            }

            toInsert.add(DailyTimetable.builder()
                    .schoolId(school.getId())
                    .academicYear(year.getName())
                    .date(date)
                    .entries(entries)
                    .build());
        }

        //! step 13 - insert them. One save for the range, inside the transaction opened above, so
        //! a refusal on the last date leaves none of the earlier ones behind.
        // TODO: insert daily timetables
        List<DailyTimetable> saved = timetables.saveAll(toInsert);

        //! step 14 - the answer. A summary rather than every period of every day: a fortnight of a
        //! four-hundred-period school is 4,000 entries the caller already holds. One date is the
        //! exception, because that is the common call and re-reading it would be a round trip for
        //! something the server had in its hand.
        List<LocalDate> createdDates = saved.stream().map(DailyTimetable::getDate).sorted().toList();

        return new TimetableCreateResponse(
                start,
                end,
                saved.size(),
                shape.size(),
                createdDates,
                skipped,
                saved.size() == 1 ? DailyTimetableResponse.of(saved.get(0)) : null,
                "Correct one period with #4 and read a day back with #7. Keep each period's "
                        + "timetableEntryId: it is what an attendance session stores and what "
                        + "every later edit addresses. " + NO_AUTHORIZATION_YET);
    }
    //! endpoint 10 — a year's days, filtered ------------------------------------------

    /**
     * Endpoint #10 — one page of a year's timetables, as counts rather than periods.
     *
     * <h2>A list carries what a list needs</h2>
     *
     * <p>A full school day measures about 120 KB, so a page of twenty carrying its periods would
     * be two and a half megabytes shipped to render twenty dates. The five counts are worked out
     * by an aggregation and the {@code entries} array never leaves the database — the same call #6
     * of grading makes about bands and #7 of positions about holders. <b>#7 is one call away</b>
     * for the caller that wants a day in full.
     *
     * <h2>The filters reach inside the periods</h2>
     *
     * <p>"Which days does this teacher work" and "which days is the lab used" are what a list of
     * days is actually opened to ask, and both live in the embedded array. A class and a section
     * are matched as <b>one period</b> rather than as two conditions — see the repository, where
     * that becomes an {@code $elemMatch}.
     *
     * <h2>No gates, and a year that has ended still answers</h2>
     *
     * <p>Reading last year's Tuesday is how a school explains an attendance record taken against
     * it. The year in the path must exist; nothing asks whether it is running.
     */
    public PageResponse<DailyTimetableSummaryResponse> listTimetables(String academicYear,
            DailyTimetableSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_DAY_FIELDS, SORTABLE_DAY_FIELD_NAMES, DAY_ORDER);

        //! step 2 - who is asking. `require`, not `requireUsable`: a suspended school still reads
        //! its own timetable.
        School school = currentSchool.require();

        //! step 3 - the year has to be one of this school's, but NOT the running one. Reading a
        //! finished year is exactly what explains an attendance record taken against it.
        AcademicYear year = utils.loadYearByName(school, academicYear);

        //! step 4 - one page, filtered, sorted and counted in the database
        // TODO: read daily timetables
        return PageResponse.from(
                timetables.search(school.getId(), year.getName(), request, pageable),
                //! Already the response type: the rows are an aggregation's output rather than
                //! documents, so there is nothing left to map.
                one -> one);
    }
}
