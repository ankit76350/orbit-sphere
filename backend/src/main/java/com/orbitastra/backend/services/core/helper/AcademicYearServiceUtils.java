package com.orbitastra.backend.services.core.helper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.embedded.HolidayDetail;
import com.orbitastra.backend.models.core.embedded.HolidayEvent;
import com.orbitastra.backend.models.core.enums.HolidayType;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

/**
 * The lookups and calendar plumbing every academic-year endpoint needs.
 *
 * <p>Lifted out of {@code AcademicYearService} so that file holds endpoints and nothing else.
 * The service had grown eight private helpers below its last endpoint, and reading it meant
 * scrolling past all of them to find the next thing that answers a request.
 *
 * <p><b>These are not validators.</b> {@link CoreValidator} refuses bad input; nothing here
 * refuses anything on its own. These find a year, find a day, count what is on it, and turn a
 * calendar into words — the questions the endpoints keep asking. The two 404s that do get thrown
 * are the answer to "which one did you mean", not a rule about what is allowed.
 *
 * <p>A {@code @Component} rather than a static utility, unlike {@link TextHelper}, because
 * {@link #loadYear} needs the repository and the tenant resolver. Splitting it — beans here,
 * statics there — would mean deciding which file each new helper belongs in every time, so they
 * all live together.
 *
 * <p>Every method says which endpoints use it, in the banner above it. The numbers are the plan
 * in {@code controllers/core/README.md}: 18 create, 19 dates, 20 replace calendar, 21 add
 * holiday, 22 edit holiday, 23 generate weekly off, 24 and 25 the enrollment gates, plus the two
 * calendar DELETEs.
 *
 * <p><b>18 and 19 barely appear below.</b> Creating a year has no year to look up, and changing
 * its dates resolves the school itself because the overlap check needs it.
 */
@Component
@RequiredArgsConstructor
public class AcademicYearServiceUtils {

    private final AcademicYearRepository academicYears;
    private final CurrentSchoolResolver currentSchool;

    //! loadYear — used by endpoints 20 to 25 and both DELETEs -------------------------

    /** The caller's year, by name, or a 404. */
    public AcademicYear loadYear(String name) {
        return loadYear(currentSchool.requireUsable(), name);
    }

    //! loadYear (school in hand) — used by nothing today, was 26 and 27 ---------------

    /**
     * The same lookup for a school already resolved.
     *
     * <p>Written for the results lock and unlock endpoints (#26, #27), which file an audit row
     * against a tenant and so need the School in hand. Those endpoints are not in the service
     * today, so nothing calls this — kept because the need returns the moment any endpoint here
     * does two things with one school, and because the alternative, resolving the tenant twice in
     * one request, is two chances to disagree.
     *
     * <p>If #26 and #27 are not coming back, delete this overload with them.
     */
    public AcademicYear loadYear(School school, String name) {
        return academicYears.findBySchoolIdAndName(school.getId(), name.trim())
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + name + "' in this school."));
    }

    //! ensureList — used by endpoints 21, 23 and both DELETEs -------------------------

    /**
     * The holiday list, created if the document has none.
     *
     * <p>Mongo stores an empty list as an empty array, but a document written before the field
     * existed comes back null — and the builder default does not apply to what is read from the
     * database.
     *
     * <p>Only the endpoints that add to the list need it. #20 does not: replacing a calendar
     * assigns a fresh list rather than adding to whatever was there.
     */
    public List<HolidayDetail> ensureList(AcademicYear year) {
        if (year.getHolidays() == null) {
            year.setHolidays(new ArrayList<>());
        }
        return year.getHolidays();
    }

    //! sizeOf — used by endpoints 20 and 23 -------------------------------------------

    /**
     * How many days the school is closed. Not the same number as the reasons for it.
     *
     * <p>The two bulk endpoints report a before-and-after count in their change summary. The
     * single-entry endpoints get their counts from {@code HolidayCalendarResponse} instead.
     */
    public int sizeOf(AcademicYear year) {
        return year.getHolidays() == null ? 0 : year.getHolidays().size();
    }

    //! eventCount — used by endpoint 23 -----------------------------------------------

    /**
     * How many reasons are recorded, which is larger whenever a day carries more than one.
     *
     * <p>#23 is the one endpoint returning a response that is not
     * {@code HolidayCalendarResponse} — which computes both counts itself — and it has to report
     * both, because a run that adds a weekly off to a day already closed for a festival raises
     * the reason count without raising the day count.
     */
    public int eventCount(AcademicYear year) {
        return ensureList(year).stream().mapToInt(d -> d.getEvents().size()).sum();
    }

    //! findDay — used by endpoints 21, 22 and DELETE one day --------------------------

    /**
     * The one entry for a date, if the school is closed that day. A date appears at most once.
     *
     * <p>The three endpoints addressed by a single date. #21 treats an absent day as "create
     * it"; the other two treat it as a 404.
     */
    public Optional<HolidayDetail> findDay(AcademicYear year, LocalDate date) {
        return ensureList(year).stream().filter(h -> h.getDate().equals(date)).findFirst();
    }

    //! hasType — used by endpoints 20 to 23 -------------------------------------------

    /**
     * Whether a day already carries a reason of this type. One day never holds two.
     *
     * <p>Every endpoint that puts a reason onto a day, which is exactly where the rule can be
     * broken. The four use the same answer differently: #20 and #21 refuse the duplicate, #22
     * refuses a retype that would collide, and #23 <b>skips</b> the date rather than refusing,
     * which is what makes running the generator twice safe.
     */
    public boolean hasType(HolidayDetail day, HolidayType type) {
        return day.getEvents().stream().anyMatch(e -> e.getType() == type);
    }

    //! resolveEvent — used by endpoint 22 and DELETE one day --------------------------

    /**
     * Which reason on a day an edit or a delete is aimed at.
     *
     * <p>With no type given, this only works when the day has exactly one reason. Where it has
     * more, it asks rather than guesses — silently editing the first of two would be wrong as
     * often as it was right, and the caller would not see it happen.
     *
     * <p>This is what {@code ?type=} means, so both endpoints that take it get the same
     * behaviour and the same two errors rather than each inventing its own.
     */
    public HolidayEvent resolveEvent(HolidayDetail day, HolidayType type) {
        List<HolidayEvent> events = day.getEvents();

        if (type == null) {
            if (events.size() > 1) {
                throw ApiException.badRequest("HOLIDAY_TYPE_REQUIRED",
                        day.getDate() + " is closed for " + events.size() + " reasons ("
                                + describe(day) + "). Add ?type= to say which one you mean.");
            }
            return events.get(0);
        }

        return events.stream()
                .filter(e -> e.getType() == type)
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("HOLIDAY_ENTRY_NOT_FOUND",
                        "No " + type + " entry on " + day.getDate() + ". That day is closed for "
                                + describe(day) + "."));
    }

    //! describe — used by endpoint 19 and DELETE one day ------------------------------

    /**
     * A day's reasons in one readable phrase, for error and change messages.
     *
     * <p>#19 is the odd caller: it names the first closed day a shrink would strand, and since a
     * date can be closed for more than one reason, naming a single holiday there would
     * under-report what is about to be stranded. Also used by both errors in
     * {@link #resolveEvent}.
     */
    public String describe(HolidayDetail day) {
        return day.getEvents().stream()
                .map(e -> e.getName() + " (" + e.getType() + ")")
                .collect(Collectors.joining(", "));
    }
}
