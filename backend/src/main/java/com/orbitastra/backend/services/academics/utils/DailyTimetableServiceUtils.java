package com.orbitastra.backend.services.academics.utils;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.dto.academics.timetable.request.TimetableEntryRequest;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.academics.timetable.embedded.TimetableEntry;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.embedded.HolidayEvent;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;

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

    /**
     * The academic year one date falls in.
     *
     * <p><b>Derived, never taken from the request</b> — rule 3 of the model's persistence contract,
     * and the reason there is no {@code {year}} in any path of this module. A year in the URL and a
     * date in the body are two sources for one fact, and the first request that disagreed with
     * itself would have no right answer.
     *
     * <p><b>A range can span two years.</b> 2027-03-30 and 2027-04-02 are four days apart and in
     * different academic years, so this is called per date rather than once per request.
     *
     * <p><b>No year containing the date is a {@code 409}, not a {@code 400}.</b> The request is
     * coherent; the school simply has not set that year up. The message says which date failed,
     * because a range refused on one of fourteen dates is otherwise a guessing game.
     *
     * Used by:
     * - createTimetables()
     */
    public AcademicYear resolveYearFor(School school, LocalDate date) {
        // TODO: read academic year
        return academicYears
                .findFirstBySchoolIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        school.getId(), date, date)
                .orElseThrow(() -> ApiException.conflict("NO_ACADEMIC_YEAR_FOR_DATE",
                        "No academic year of this school contains " + date + ". A timetable "
                                + "belongs to a year, and the year is worked out from the date "
                                + "rather than sent."));
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
     * @return the holiday's name when the date is a non-working day, otherwise null
     *
     * Used by:
     * - createTimetables()
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
