package com.orbitastra.backend.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central check that a date belongs to an academic year. <b>Every module's, not one module's.</b>
 *
 * <p><b>Modelled on {@link com.orbitastra.backend.common.access.ActionGate}</b>: one shared
 * {@code @Component}, {@code require...} methods that either return quietly or throw an
 * {@link ApiException} saying exactly what is wrong, and one place for a rule that has to read the
 * same way everywhere.
 *
 * <p><b>It was the admission module's until 2026-09-28</b>, and the same arithmetic was written out
 * four more times elsewhere: holidays and working days in core, terms in academics, and daily
 * timetables twice. Each had its own wording, its own inclusive-or-exclusive decision at the
 * boundary, and no way of noticing when one of them drifted. They all come through here now.
 *
 * <h2>What a caller chooses, and what it does not</h2>
 *
 * <p><b>THE BOUND</b> — {@link Bound#INSIDE_THE_YEAR} for a date that must sit within the year at
 * both ends, {@link Bound#NOT_AFTER_THE_END} for one that may run ahead of it. Admissions are the
 * second kind: a school takes enquiries and applications for 2026-2027 in the months running up to
 * it, and refusing that refuses an ordinary calendar. A holiday, a term and a timetable are the
 * first kind — they describe days the year itself covers.
 *
 * <p><b>THE REFUSAL</b> — its {@link Refusal#code() code} and whether it is a 400 or a 409. A
 * cycle's is {@code 400 CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR}; a term's is
 * {@code 409 TERM_OUTSIDE_ACADEMIC_YEAR}. The <i>rule</i> is shared and the <i>name a caller's API
 * gives it</i> is not — a single code here would make every module's error table describe somebody
 * else's endpoint, and callers could not document their own refusals honestly.
 *
 * <p><b>THE FIELD NAMES</b> — the map's keys, which is what the message blames. A caller passing
 * {@code "enrollmentDeadlineAt"} or {@code "Republic Day"} gets that back in the refusal.
 *
 * <p><b>What it does NOT let a caller choose is the arithmetic</b>, which is the whole point.
 *
 * <h2>Decisions worth keeping</h2>
 *
 * <p><b>It is NOT in {@code common.access}, beside {@code ActionGate}, and that is deliberate.</b>
 * Every gate in that class answers "may this caller act at all" — the school, the subscription, the
 * running year — and the project's rule is that those are called from the <b>controller</b>, under
 * their {@code Gate N} banner. This one cannot be: it checks <b>values</b>, and a PATCH's values
 * are the stored ones merged with the sent ones, which only the service has. Filing it with the
 * gates would invite the controller-only rule to be applied to it and be wrong every time. It
 * lives beside {@link Dates}, whose job it also shares.
 *
 * <p><b>The whole of the last day counts, in the school's own zone.</b> A year's {@code endDate} is
 * a {@code LocalDate}, so an instant at 18:00 on the final day is inside the year rather than a day
 * past it — and 20:00Z on that day is already <i>tomorrow</i> for a school in Kolkata, so it is
 * not. Comparing against UTC midnight would refuse the last afternoon of every year for every
 * school east of Greenwich. A {@code LocalDate} needs no zone and is compared as it is.
 *
 * <p><b>A year with no bounds is not checked.</b> Both are {@code @NotNull} on the model, but
 * nothing enforces that on save, and refusing every date because a year predates the rule would
 * break the schools least able to fix it.
 *
 * <p><b>An absent date is not an offence.</b> A map entry with no value is a field the caller did
 * not set, and a check is not the place to decide whether it had to be.
 *
 * <p><b>One pass, in the map's order</b>, so the first offending field is the one reported — a
 * caller who moved four dates by a year wants to be told once.
 *
 * <p><b>Two shapes and one loader, and it has had seven methods.</b> Every overload that existed
 * for convenience rather than for a caller has been deleted, twice: an unused method whose
 * behaviour no test reaches is where a wrong answer hides. A module that wants another form adds
 * it then, with a test behind it.
 */
@Component
@RequiredArgsConstructor
public class AcademicYearWindow {

    /** Which end of the year a caller's dates have to respect. */
    public enum Bound {

        /**
         * On or after the first day, on or before the last. What a holiday, a term and a
         * timetable need: they describe days the year itself covers.
         */
        INSIDE_THE_YEAR,

        /**
         * On or before the last day, however far ahead of the year it is. What admissions need: a
         * school runs a whole round in the months running up to the year it admits for.
         *
         * <p>The upper half is the one that catches mistakes either way. A date before the year is
         * a school planning ahead; a date after it is a typo, and a deadline in 2099 is what this
         * refuses.
         */
        NOT_AFTER_THE_END
    }

    /**
     * What the caller's API calls this, and which status it answers with.
     *
     * <p>Built by name — {@code Refusal.conflict("TERM_OUTSIDE_ACADEMIC_YEAR")} — so a call site
     * reads as the refusal it produces rather than as a boolean nobody can decode.
     */
    public record Refusal(String code, boolean asConflict) {

        /** A value the caller sent is wrong. */
        public static Refusal badRequest(String code) {
            return new Refusal(code, false);
        }

        /** The state of the thing being written disagrees with the year. */
        public static Refusal conflict(String code) {
            return new Refusal(code, true);
        }
    }

    private final AcademicYearRepository academicYears;

    /**
     * Refuses any of {@code dates} that does not respect {@code bound}, for a caller that has only
     * the year's <b>name</b> — which other collections store rather than its id.
     *
     * <p>Used by:
     * - AdmissionCycleService.createCycle()
     * - AdmissionCycleService.updateCycle()
     * - AdmissionCycleService.moveStatus()
     *
     * @throws ApiException {@code 404 ACADEMIC_YEAR_NOT_FOUND} when the school has no such year
     */
    public void requireDates(School school, String academicYearName, SchoolTimeZone zone,
            Bound bound, Refusal refusal, Map<String, Instant> dates) {

        //! step 1 - the year, which has to be one this school actually has
        // TODO: read academic year
        AcademicYear year = academicYears
                .findBySchoolIdAndName(school.getId(), academicYearName)
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + academicYearName + "' in this school."));

        //! step 2 - the dates
        requireDates(year, zone, bound, refusal, dates);
    }

    /**
     * Refuses any of {@code dates} — <b>instants</b> — that does not respect {@code bound}.
     *
     * <p>Used by: requireDates(School, ...), and every caller of it.
     */
    public void requireDates(AcademicYear year, SchoolTimeZone zone, Bound bound, Refusal refusal,
            Map<String, Instant> dates) {

        if (year.getStartDate() == null || year.getEndDate() == null) {
            return;
        }

        //! THE INSTANT THE YEAR BEGINS AND THE ONE THE DAY AFTER IT ENDS, both in the school's own
        //! zone. Anything strictly before the second happened on or before the last day.
        Instant opens = year.getStartDate().atStartOfDay(zone.toZoneId()).toInstant();
        Instant closes = year.getEndDate().plusDays(1).atStartOfDay(zone.toZoneId()).toInstant();

        for (Map.Entry<String, Instant> each : dates.entrySet()) {
            Instant when = each.getValue();
            if (when == null) {
                continue;
            }
            boolean late = !when.isBefore(closes);
            boolean early = bound == Bound.INSIDE_THE_YEAR && when.isBefore(opens);
            if (late || early) {
                throw refuse(refusal, year, each.getKey(), Dates.readable(when, zone), late);
            }
        }
    }

    /**
     * Refuses any of {@code dates} — <b>calendar days</b> — that does not respect {@code bound}.
     *
     * <p><b>No zone, because a {@code LocalDate} has no moment in it.</b> A holiday on the year's
     * last day is that day in every zone, and passing one in would invite a caller to convert
     * twice.
     *
     * <p>Used by:
     * - AcademicYearService.countWorkingDays() / getDayStatus() / addHoliday()
     *   / generateWeeklyOff() / replaceCalendar()
     * - AcademicTermService.createTerm() / updateTerm()
     * - DailyTimetableService.getRange() / putDay() / copyDay()
     */
    public void requireDates(AcademicYear year, Bound bound, Refusal refusal,
            Map<String, LocalDate> dates) {

        if (year.getStartDate() == null || year.getEndDate() == null) {
            return;
        }

        for (Map.Entry<String, LocalDate> each : dates.entrySet()) {
            LocalDate when = each.getValue();

            //! NOT REACHED BY ANY CALLER TODAY, AND MEASURED. A mutation that made a missing date
            //! an offence here broke nothing: every calendar-day caller passes a value that is
            //! @NotNull on its request or defaulted from the year before it gets here. The
            //! instant form's twin IS reached — #2 checks a cycle made before the four dates were
            //! required — and both read the same way on purpose. This stays because deleting it
            //! turns the first nullable field anybody adds into a NullPointerException.
            if (when == null) {
                continue;
            }
            boolean late = when.isAfter(year.getEndDate());
            boolean early = bound == Bound.INSIDE_THE_YEAR && when.isBefore(year.getStartDate());
            if (late || early) {
                throw refuse(refusal, year, each.getKey(), Dates.readable(when), late);
            }
        }
    }

    /**
     * The refusal itself, built the same way whichever shape found the problem.
     *
     * <p><b>Private, so it is not one of this component's methods calling another.</b> That rule
     * is about the surface callers use; this is the file's own wording, kept in one place so an
     * instant and a calendar day cannot come back described differently.
     *
     * <p><b>The message says which half was broken.</b> "after the year ends" and "outside the
     * year" are different mistakes — one is a typo, the other is usually the wrong year — and a
     * caller allowed to run early must never be told its date is "outside" anything.
     */
    private ApiException refuse(Refusal refusal, AcademicYear year, String field, String when,
            boolean late) {

        String message = field + " is " + when + ", which is "
                + (late
                        ? "after '" + year.getName() + "' ends. That year ends "
                                + Dates.readable(year.getEndDate()) + "."
                        : "outside '" + year.getName() + "'. That year runs "
                                + Dates.readable(year.getStartDate()) + " to "
                                + Dates.readable(year.getEndDate()) + ".");

        return refusal.asConflict()
                ? ApiException.conflict(refusal.code(), message)
                : ApiException.badRequest(refusal.code(), message);
    }

}
