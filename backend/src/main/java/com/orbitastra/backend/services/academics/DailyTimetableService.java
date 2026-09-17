package com.orbitastra.backend.services.academics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.academics.timetable.request.DailyTimetableCreateRequest;
import com.orbitastra.backend.dto.academics.timetable.response.DailyTimetableResponse;
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
 * <p><b>The gates are split, and that is this module's one deviation from the project rule.</b>
 * Gates 1 and 2 run in the controller as everywhere else. Gate 4 — is this year the running one —
 * cannot: the year is derived from a date in the <i>body</i>, and a range may span two years, so
 * there is nothing for the controller to ask about before the service has read the request. The
 * equivalent check is per date, below, and it is a refusal rather than a gate.
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
    public TimetableCreateResponse createTimetables(DailyTimetableCreateRequest request) {

        //! step 1 - who is asking
        School school = currentSchool.requireUsable();

        //! step 2 - the range, settled before anything is read. An absent endDate means one day.
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

        List<LocalDate> dates = start.datesUntil(end.plusDays(1)).toList();

        //! step 3 - no date in the range may already have a timetable. ONE query for the whole
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

        //! step 4 - build the entries ONCE, before any of the shape checks, so that what is
        //! validated is what will be stored rather than what was sent. The same order #1 of
        //! grading settled on for bands.
        //!
        //! The ids here are placeholders: each date gets its own set in step 8, because two dates
        //! sharing an entry id would make AttendanceSession.timetableEntryId ambiguous.
        List<TimetableEntry> shape = request.entries().stream()
                .map(utils::toEntry)
                .toList();

        //! step 5 - the rules that depend only on the periods themselves, run once for the whole
        //! range because every date carries the same set. Order matters: an inverted period
        //! checked for overlap reports a clash, which is true and names the wrong problem.
        helper.validateTimes(shape);
        helper.validateSlotFields(shape);
        helper.validatePeriodCodesUnique(shape);
        helper.validateNoSectionOverlap(shape);
        helper.validateNoTeacherOverlap(shape);
        helper.validateNoRoomOverlap(shape);

        //! step 6 - every teacher named has to be this school's. Read in ONE query rather than one
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

        //! step 7 - which dates are working days, and which year each belongs to. A range can
        //! cross a year boundary, so the year is resolved per date rather than once.
        Map<LocalDate, AcademicYear> yearOf = new LinkedHashMap<>();
        List<SkippedDateResponse> skipped = new ArrayList<>();

        for (LocalDate date : dates) {
            AcademicYear year = utils.resolveYearFor(school, date);

            //! GATE 4'S EQUIVALENT, and the reason it is here rather than in the controller: the
            //! year comes from a date in the body, and a range may span two of them.
            if (!Boolean.TRUE.equals(year.getIsThisYearRunning())) {
                throw ApiException.conflict("ACADEMIC_YEAR_NOT_RUNNING",
                        "'" + year.getName() + "' is not this school's running year, so nothing "
                                + "can be scheduled into it. " + date + " falls inside it.");
            }

            //! A HOLIDAY IS SKIPPED, NOT REFUSED - see the class note. Nothing about the day of
            //! the week is consulted: a school that runs on Sunday is a normal school.
            String holiday = utils.holidayNameFor(year, date);
            if (holiday != null) {
                skipped.add(new SkippedDateResponse(date, "NOT_A_WORKING_DAY", holiday));
                continue;
            }

            yearOf.put(date, year);
        }

        //! step 8 - nothing to write means every date was a holiday. A refusal rather than an
        //! empty success: the caller asked for a timetable and has none, and a 201 saying "0
        //! created" reads as though something worked.
        //!
        //! Checked BEFORE the structure below, so a request aimed entirely at holidays hears about
        //! that rather than about a class it was never going to be written against.
        if (yearOf.isEmpty()) {
            throw ApiException.conflict("NOT_A_WORKING_DAY",
                    "Every date from " + start + " to " + end + " is a holiday or weekly off for "
                            + "this school, so no timetable was written.");
        }

        //! step 9 - the structure every period has to fit: the class, the section, and a subject
        //! that section actually studies.
        //!
        //! ONCE PER YEAR, NOT ONCE PER DATE. The periods are identical for every date and a class
        //! belongs to one academic year, so the answer can only differ when the year does. A
        //! 120-day range inside one year asked these questions 120 times before 2026-09-16.
        Set<String> years = new LinkedHashSet<>();
        for (AcademicYear year : yearOf.values()) {
            years.add(year.getName());
        }

        for (String yearName : years) {
            //! One read per CLASS, not one per period: a day of four hundred periods across twelve
            //! classes is twelve queries. The cache is per year, because the same id in two years
            //! is two different classes.
            Map<String, SchoolClass> classes = new LinkedHashMap<>();

            for (TimetableEntry entry : shape) {
                SchoolClass schoolClass = classes.get(entry.getClassDocsId());
                if (schoolClass == null) {
                    schoolClass = utils.loadClassForYear(school, yearName, entry.getClassDocsId());
                    classes.put(entry.getClassDocsId(), schoolClass);
                }

                helper.validateSectionIsActive(schoolClass, entry.getSectionNo());
                helper.validateSubjectForSection(schoolClass, entry.getSubjectCode(),
                        entry.getSectionNo());
            }
        }

        //! step 10 - build a document per working date. Fresh entry ids for each: they are
        //! different periods on different days, and two dates sharing an id would make
        //! AttendanceSession.timetableEntryId ambiguous.
        //!
        //! schoolId explicitly, like every write in this project - nothing validates a document on
        //! save, and one written without it is invisible to every tenant-scoped query afterwards.
        List<DailyTimetable> toInsert = new ArrayList<>();

        for (Map.Entry<LocalDate, AcademicYear> working : yearOf.entrySet()) {
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
                    .academicYear(working.getValue().getName())
                    .date(working.getKey())
                    .entries(entries)
                    .build());
        }

        //! step 11 - insert them. One save for the range, inside the transaction opened above, so
        //! a refusal on the last date leaves none of the earlier ones behind.
        // TODO: insert daily timetables
        List<DailyTimetable> saved = timetables.saveAll(toInsert);

        //! step 12 - the answer. A summary rather than every period of every day: a fortnight of a
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
}
