package com.orbitastra.backend.services.academics.utils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableEntryReplaceRequest;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableEntryRequest;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.embedded.HolidayEvent;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;
import com.orbitastra.backend.services.academics.helper.TimetableHelper;

import lombok.RequiredArgsConstructor;

/**
 * The reads {@link com.orbitastra.backend.services.academics.DailyTimetableService} makes more than
 * once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and a method here
 * never calls another method here.
 */
@Component
@RequiredArgsConstructor
public class DailyTimetableServiceUtils {

    private final AcademicYearRepository academicYears;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;

    //! UTILS MAY REACH THE HELPER, and only in this direction. The folder rule is
    //! service -> utils -> helper: a helper never calls another helper, and nothing calls back up.
    //! normaliseAgainstStructure needs two of the helper's rules in the middle of a read loop, and
    //! splitting the loop so the service could interleave them would put the same six lines in #1
    //! and #2 - which is the duplication this method exists to remove.
    private final TimetableHelper helper;

    /**
     * One academic year of this school, by the name in the URL.
     *
     * <h2>The year is stated, not worked out from the date — changed 2026-09-17</h2>
     *
     * <p><b>This reverses rule 3 of the model's persistence contract</b>, which said the year
     * "is derived from {@code date}, not trusted from the request". Deriving it was defensible
     * and turned out to be wrong in practice: a school with two years whose ranges both contain a
     * September date had a timetable silently written into the year it did not pick, and the
     * refusal it eventually got named a year it had never mentioned.
     *
     * <p><b>Stating it makes the mismatch visible instead of resolving it quietly.</b> The caller
     * says which year it means, and a date outside that year is
     * {@code 409 DATE_OUTSIDE_ACADEMIC_YEAR} rather than a timetable in a different year. The
     * derivation had no way to express "you asked for the wrong thing", because every date
     * resolves to <i>something</i>.
     *
     * <p>It also puts this module back on the shape the rest of {@code academics} uses —
     * {@code /academic-years/{year}/…} — which is what lets gate 4 run in the controller like
     * everywhere else, instead of the per-date check this service used to carry.
     *
     * Used by:
     * - createTimetables()
     * - getTimetable()
     * - listTimetables()
     */
    public AcademicYear loadYearByName(School school, String academicYear) {
        String name = academicYear == null ? "" : academicYear.trim();

        // TODO: read academic year
        return academicYears.findBySchoolIdAndName(school.getId(), name)
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + name + "' in this school."));
    }

    /**
     * One class of this school, in one academic year.
     *
     * <p><b>Scoped by the year as well as the school</b>, because a class belongs to exactly one:
     * a real class id from 2025-2026 is a real id, and scheduling this year's Tuesday against it
     * would build a timetable for a class that no longer runs. That is why the finder takes three
     * arguments where most take two.
     *
     * <p><b>A retired class is returned</b>, deliberately. Whether a timetable may be written
     * against one is not this read's question — the section and the subject carry their own
     * {@code active} flags, and those are what a period actually names.
     *
     * Used by:
     * - createTimetables()
     */
    public SchoolClass loadClassForYear(School school, String academicYear, String classDocsId) {
        // TODO: read school class
        return schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(classDocsId, school.getId(), academicYear)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND",
                        "No class with id '" + classDocsId + "' in '" + academicYear + "'."));
    }

    /**
     * One request period as it is stored.
     *
     * <p><b>A mapper rather than a read</b>, which is the one thing here that is not a query — and
     * it belongs beside them for the reason {@code StaffServiceUtils.toAddress} and
     * {@code toEmergencyContact} do: turning a request record into the embedded document it will
     * be saved as is work the service should not be doing in the middle of its steps.
     *
     * <p><b>The generated id is a placeholder.</b> #1 replaces it per date, because two dates
     * sharing an entry id would make {@code AttendanceSession.timetableEntryId} ambiguous. One is
     * set here anyway so that what the checks run against is a fully-formed document rather than
     * one with a hole in it — the same reason #1 builds before it validates.
     *
     * <p><b>A create can never be handed an id</b>, which is why the request has no field for one.
     * The overload below takes {@link TimetableEntryReplaceRequest} and keeps the id it is given:
     * #2 is the one write where the caller legitimately knows one.
     *
     * <p><b>An empty string is not a value.</b> {@link TextHelper#blankToNull} decides that, and
     * it is reached rather than reimplemented: a fourth copy of a one-line trim is a fourth place
     * for the rule to drift.
     *
     * Used by:
     * - createTimetables()
     */
    public TimetableEntry toEntry(TimetableEntryRequest entry) {
        return TimetableEntry.builder()
                .id(new ObjectId().toHexString())
                .periodCode(entry.periodCode().trim())
                .classDocsId(entry.classDocsId().trim())
                .sectionNo(entry.sectionNo().trim())
                .slotType(entry.slotType())
                .subjectCode(TextHelper.blankToNull(entry.subjectCode()))
                .teacherDocsId(TextHelper.blankToNull(entry.teacherDocsId()))
                .slotLabel(TextHelper.blankToNull(entry.slotLabel()))
                .startTime(entry.startTime())
                .endTime(entry.endTime())
                .facilityResourceDocsId(TextHelper.blankToNull(entry.facilityResourceDocsId()))
                .build();
    }

    /**
     * One period of a <b>replace</b> as it is stored, keeping the id it came in with.
     *
     * <p><b>The id is taken as given, and that is the difference from {@link #toEntry}.</b> #2
     * preserves a surviving period's identity so that an {@code AttendanceSession} pointing at it
     * still points at it; an absent id means a period being added, and gets a fresh one. Whether a
     * supplied id is really <i>this day's</i> is checked in the service, where the stored day is in
     * hand — this method does not read anything.
     *
     * <p>Blank is absent. A {@code timetableEntryId} of {@code ""} means "new", the same as leaving
     * the field off, because a stored id is never empty.
     *
     * Used by:
     * - replaceTimetable()
     */
    public TimetableEntry toEntry(TimetableEntryReplaceRequest entry) {
        String id = TextHelper.blankToNull(entry.timetableEntryId());

        return TimetableEntry.builder()
                .id(id == null ? new ObjectId().toHexString() : id)
                .periodCode(entry.periodCode().trim())
                .classDocsId(entry.classDocsId().trim())
                .sectionNo(entry.sectionNo().trim())
                .slotType(entry.slotType())
                .subjectCode(TextHelper.blankToNull(entry.subjectCode()))
                .teacherDocsId(TextHelper.blankToNull(entry.teacherDocsId()))
                .slotLabel(TextHelper.blankToNull(entry.slotLabel()))
                .startTime(entry.startTime())
                .endTime(entry.endTime())
                .facilityResourceDocsId(TextHelper.blankToNull(entry.facilityResourceDocsId()))
                .build();
    }

    /**
     * Every period fitted to the school's own structure, and its section spelled the class's way.
     *
     * <h2>It normalises as well as validates, and the order that puts it in is load-bearing</h2>
     *
     * <p>A section is resolved against its class <b>case-insensitively</b>, so {@code "A"} and
     * {@code "a"} name one section. Until 2026-09-17 the overlap checks then compared the two
     * strings, saw two sections, and let 10-A be given two periods at 09:30 — <b>accepted, 201</b>.
     * Replacing what was sent with the class's own spelling here means everything downstream
     * compares one spelling, and the day is stored with one spelling per section.
     *
     * <p>So this must run <b>before</b> the shape checks, in #1 and in #2 alike. That is the whole
     * reason it is one method rather than three calls the service interleaves.
     *
     * <h2>One read per class, not one per period</h2>
     *
     * <p>A day of four hundred periods across twelve classes is twelve queries. The classes are
     * cached by id as they are resolved, which is what makes the per-period loop cheap.
     *
     * Used by:
     * - createTimetables()
     * - replaceTimetable()
     */
    public void normaliseAgainstStructure(School school, AcademicYear year,
            List<TimetableEntry> entries) {

        Map<String, SchoolClass> classes = new LinkedHashMap<>();

        for (TimetableEntry entry : entries) {
            SchoolClass schoolClass = classes.get(entry.getClassDocsId());
            if (schoolClass == null) {
                schoolClass = loadClassForYear(school, year.getName(), entry.getClassDocsId());
                classes.put(entry.getClassDocsId(), schoolClass);
            }

            //! THE CLASS'S OWN SPELLING replaces whatever was sent. A section is its sectionNo -
            //! what an AttendanceSession stores and what every later read looks up by - so two
            //! spellings of one section in a day means a lookup by either finds half the periods.
            entry.setSectionNo(helper.requireActiveSection(schoolClass, entry.getSectionNo()));

            helper.validateSubjectForSection(schoolClass, entry.getSubjectCode(),
                    entry.getSectionNo());
        }
    }

    /**
     * Every teacher named anywhere in the day has to be this school's.
     *
     * <p><b>One query, not one per period.</b> A day of four hundred periods would otherwise be
     * four hundred round trips; the ids are collected first and asked for together.
     *
     * <p><b>The refusal names the id that was not found</b>, rather than reporting a count. A
     * caller sent a specific wrong id and can only act on being told which.
     *
     * <p>A period with no teacher contributes nothing: that is a break nobody supervises, which is
     * legal.
     *
     * Used by:
     * - createTimetables()
     * - replaceTimetable()
     */
    public void requireTeachersExist(School school, List<TimetableEntry> entries) {
        Set<String> wanted = new LinkedHashSet<>();
        for (TimetableEntry entry : entries) {
            if (entry.getTeacherDocsId() != null) {
                wanted.add(entry.getTeacherDocsId());
            }
        }

        if (wanted.isEmpty()) {
            return;
        }

        // TODO: read staff
        Set<String> found = new LinkedHashSet<>();
        for (Staff person : staff.findBySchoolIdAndIdIn(school.getId(), wanted)) {
            found.add(person.getId());
        }

        for (String id : wanted) {
            if (!found.contains(id)) {
                throw ApiException.notFound("TEACHER_NOT_FOUND",
                        "No staff member with id '" + id + "' in this school.");
            }
        }
    }

    /**
     * What the school calls this date, when it is not a working day.
     *
     * <h2>A weekly off is a dated holiday, not a weekday</h2>
     *
     * <p><b>Nothing here looks at the day of the week, and nothing ever may.</b> A school that runs
     * on Sunday and closes on Friday is a normal school, and the only thing that knows which is
     * {@code AcademicYear.holidays} — a list of dated {@link HolidayDetail} rows whose events carry
     * a {@link com.orbitastra.backend.models.core.enums.HolidayType}, one of which is
     * {@code WEEKLY_OFF}. The type says <i>why</i> the school is closed; the date says <i>that</i>
     * it is.
     *
     * <p><b>Both a write and a read ask this.</b> #1 skips a holiday inside a range and names it;
     * #7 uses the same answer to say <i>why</i> a date has no timetable, because "nothing was
     * written" and "the school was closed" are different facts and only one needs acting on.
     *
     * @return the holiday's name when the date is a non-working day, otherwise null
     *
     * Used by:
     * - createTimetables()
     * - getTimetable()
     */
    public String holidayNameFor(AcademicYear year, LocalDate date) {
        if (year.getHolidays() == null) {
            return null;
        }

        for (HolidayDetail holiday : year.getHolidays()) {
            if (!date.equals(holiday.getDate())) {
                continue;
            }

            //! The first event's name is what a person would call the day. A date carrying several
            //! - a festival that is also the weekly off - is named by the first, because the
            //! response is telling a human why nothing was written rather than enumerating causes.
            if (holiday.getEvents() != null) {
                for (HolidayEvent event : holiday.getEvents()) {
                    if (event.getName() != null && !event.getName().isBlank()) {
                        return event.getName();
                    }
                }
            }
            return "Non-working day";
        }
        return null;
    }
}
