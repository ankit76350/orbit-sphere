package com.orbitastra.backend.common.time;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.core.academicyear.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central check that a date belongs inside an academic year.
 *
 * <p><b>Modelled on {@link com.orbitastra.backend.common.access.ActionGate}</b>: one shared
 * {@code @Component}, {@code require...} methods that either return quietly or throw an
 * {@link ApiException} saying exactly what is wrong, and one place for a rule that has to read the
 * same way in every module.
 *
 * <p><b>THE CALLER SUPPLIES THE ERROR CODE.</b> A cycle's dates outside their year is
 * {@code CYCLE_DATE_OUTSIDE_ACADEMIC_YEAR}; a term's will be something else, and a fee schedule's
 * something else again. The <i>rule</i> is shared and the <i>name a caller's API gives it</i> is
 * not — a single code here would make every module's refusals read as if they came from somewhere
 * else, and callers could not document their own error tables honestly.
 *
 * <p><b>It is NOT in {@code common.access}, beside {@code ActionGate}, and that is deliberate.</b>
 * Every gate in that class answers "may this caller act at all" — the school, the subscription, the
 * running year — and the project's rule is that those are called from the <b>controller</b>, under
 * their {@code Gate N} banner. This one cannot be: it checks <b>values</b>, and a PATCH's values
 * are the stored ones merged with the sent ones, which only the service has. Putting it next to
 * {@code ActionGate} would invite the controller-only rule to be applied to it and be wrong every
 * time. It lives beside {@link Dates}, whose job it also shares.
 *
 * <p><b>The whole of the last day counts, in the school's own zone.</b> A year's {@code endDate} is
 * a {@code LocalDate}, so a deadline at 18:00 on the final day is inside the year rather than a day
 * past it — and 20:00Z on that day is already <i>tomorrow</i> for a school in Kolkata, so it is
 * not. Comparing against UTC midnight would refuse the last afternoon of every year for every
 * school east of Greenwich.
 *
 * <p><b>A year with no start or end is not checked.</b> Both are {@code @NotNull} on the model, but
 * nothing enforces that on save, and refusing every date because a year predates the rule would
 * break the schools least able to fix it.
 *
 * <p><b>400, not 409.</b> A date outside the year is a value the caller sent, not a state the
 * school is in — which is the line the rest of this project draws between the two.
 *
 * <p><b>Two methods, and it had five.</b> Three were convenience overloads nobody called — a
 * single-date form, and two that defaulted the early-allowed set to empty. A mutation proved the
 * point: changing one of those defaults to "everything may run early" broke nothing, because no
 * path reached it. An unused overload whose default is untested is where a wrong default hides,
 * so they were deleted on 2026-09-28. A module that wants a simpler form adds it then, with a
 * test behind it.
 */
@Component
@RequiredArgsConstructor
public class AcademicYearWindow {

    private final AcademicYearRepository academicYears;

    /**
     * Refuses any of {@code dates} outside the named year, bar the ones that may precede it.
     *
     * <p>The load-by-name form of the overload below, for callers that have only the name — which
     * is most of them, because a year's name is what other collections store.
     *
     * @throws ApiException {@code 404 ACADEMIC_YEAR_NOT_FOUND} when the school has no such year
     */
    public void requireInside(School school, String academicYearName, SchoolTimeZone zone,
            String errorCode, Map<String, Instant> dates, Set<String> mayPrecedeTheYear) {

        //! step 1 - the year, which has to be one this school actually has
        // TODO: read academic year
        AcademicYear year = academicYears
                .findBySchoolIdAndName(school.getId(), academicYearName)
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + academicYearName + "' in this school."));

        //! step 2 - the dates
        requireInside(year, zone, errorCode, dates, mayPrecedeTheYear);
    }

    /**
     * Refuses any of {@code dates} that falls outside this academic year — except the ones named
     * in {@code mayPrecedeTheYear}, which may be <b>before it starts</b> but still not after it
     * ends.
     *
     * <p><b>A school opens admissions for a year before that year begins.</b> Enquiries and
     * applications for 2026-2027 are taken in the months running up to it; the family is choosing
     * a school they will join later. Requiring every date inside the year refuses the ordinary
     * admissions calendar, which is the first thing this check got wrong.
     *
     * <p><b>Nothing may be after the year ends, ever.</b> That half is what catches the 2099
     * deadline, and it is the half worth keeping: a date before the year is a school planning
     * ahead, and a date after it is a typo.
     *
     * <p><b>Which fields may run early is the CALLER'S rule, not this component's.</b> A cycle's
     * opening dates may; its closing ones may not. Another module will draw the line somewhere
     * else, and a set passed in is how it says where.
     *
     * <p><b>One pass, in the map's order</b>, so the first offending field is the one reported —
     * a caller who moved four dates by a year wants to be told once.
     */
    public void requireInside(AcademicYear year, SchoolTimeZone zone, String errorCode,
            Map<String, Instant> dates, Set<String> mayPrecedeTheYear) {

        //! step 1 - a year that does not say when it runs cannot judge anything. Both dates are
        //! @NotNull on the model and nothing enforces it on save, so this is reachable.
        if (year.getStartDate() == null || year.getEndDate() == null) {
            return;
        }

        //! step 2 - the window, in the SCHOOL'S zone and inclusive of the whole last day. Against
        //! UTC midnight this would refuse the last afternoon of every year east of Greenwich.
        Instant opens = year.getStartDate().atStartOfDay(zone.toZoneId()).toInstant();
        Instant closes = year.getEndDate().plusDays(1).atStartOfDay(zone.toZoneId()).toInstant();

        //! step 3 - the first one outside it, and nothing after that.
        for (Map.Entry<String, Instant> each : dates.entrySet()) {
            Instant when = each.getValue();
            if (when == null) {
                continue;
            }

            //! A FIELD THAT MAY RUN EARLY IS ONLY CHECKED AGAINST THE END. Admissions open before
            //! the year they admit for — that is the calendar, not a mistake.
            boolean early = mayPrecedeTheYear.contains(each.getKey());

            if (!when.isBefore(closes) || (!early && when.isBefore(opens))) {
                throw ApiException.badRequest(errorCode,
                        each.getKey() + " is " + Dates.readable(when, zone) + ", which is "
                                + (early ? "after '" + year.getName() + "' ends. That year ends "
                                        + Dates.readable(year.getEndDate()) + "."
                                        : "outside '" + year.getName() + "'. That year runs "
                                        + Dates.readable(year.getStartDate()) + " to "
                                        + Dates.readable(year.getEndDate()) + "."));
            }
        }
    }

}
