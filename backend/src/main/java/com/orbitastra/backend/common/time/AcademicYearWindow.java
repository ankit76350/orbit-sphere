package com.orbitastra.backend.common.time;

import java.time.Instant;
import java.util.Map;

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
 */
@Component
@RequiredArgsConstructor
public class AcademicYearWindow {

    private final AcademicYearRepository academicYears;

    /**
     * Refuses any of {@code dates} that falls outside the named academic year.
     *
     * <p>The overload that loads the year by name, for callers that have only the name — which is
     * most of them, because a year's name is what other collections store. {@code ActionGate} has
     * the same pair.
     *
     * @throws ApiException {@code 404 ACADEMIC_YEAR_NOT_FOUND} when the school has no such year
     */
    public void requireInside(School school, String academicYearName, SchoolTimeZone zone,
            String errorCode, Map<String, Instant> dates) {

        //! step 1 - the year, which has to be one this school actually has
        // TODO: read academic year
        AcademicYear year = academicYears
                .findBySchoolIdAndName(school.getId(), academicYearName)
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + academicYearName + "' in this school."));

        //! step 2 - the dates
        requireInside(year, zone, errorCode, dates);
    }

    /**
     * Refuses any of {@code dates} that falls outside this academic year.
     *
     * <p><b>The order of the map decides which failure is reported</b>, so a caller that wants its
     * fields named in a particular order passes a {@code LinkedHashMap}. Only the first one out of
     * range is reported: a caller who moved four dates by a year wants to be told that once.
     *
     * <p><b>A null value is skipped, not refused.</b> Whether a date is required is the caller's
     * rule, and it has already been checked by the time anything gets here.
     *
     * @param zone      the school's own, because the last day is a local day
     * @param errorCode the caller's name for this refusal — see the class doc
     * @param dates     field name to instant; the field name is what the message says
     */
    public void requireInside(AcademicYear year, SchoolTimeZone zone, String errorCode,
            Map<String, Instant> dates) {

        //! step 1 - a year that does not say when it runs cannot judge anything. Both dates are
        //! @NotNull on the model and nothing enforces it on save, so this is reachable.
        if (year.getStartDate() == null || year.getEndDate() == null) {
            return;
        }

        //! step 2 - the window, in the SCHOOL'S zone and inclusive of the whole last day. Against
        //! UTC midnight this would refuse the last afternoon of every year east of Greenwich.
        Instant opens = year.getStartDate().atStartOfDay(zone.toZoneId()).toInstant();
        Instant closes = year.getEndDate().plusDays(1).atStartOfDay(zone.toZoneId()).toInstant();

        //! step 3 - the first one outside it, and nothing after that. Four dates moved by a year
        //! is one mistake, and a caller wants to be told about it once.
        for (Map.Entry<String, Instant> each : dates.entrySet()) {
            Instant when = each.getValue();
            if (when == null) {
                continue;
            }
            if (when.isBefore(opens) || !when.isBefore(closes)) {
                throw ApiException.badRequest(errorCode,
                        each.getKey() + " is " + Dates.readable(when, zone) + ", which is outside '"
                                + year.getName() + "'. That year runs "
                                + Dates.readable(year.getStartDate()) + " to "
                                + Dates.readable(year.getEndDate()) + ".");
            }
        }
    }

    /**
     * Refuses one date that falls outside this academic year.
     *
     * <p>The single-date form, for the callers that have one — a term's start, a fee's due date.
     * It delegates rather than repeating the window arithmetic; {@code ActionGate}'s overloads do
     * the same, and the flat-helper rule this project applies to service {@code utils} is about
     * those, not about a shared component's own overloads.
     */
    public void requireInside(AcademicYear year, SchoolTimeZone zone, String errorCode,
            String field, Instant when) {

        //! NULL IS SKIPPED, as it is in the map form. Map.of refuses a null value, so the guard
        //! is here rather than there — and EPOCH as a stand-in would be a date outside every
        //! academic year ever written, which is the opposite of skipping it.
        if (when == null) {
            return;
        }

        requireInside(year, zone, errorCode, Map.of(field, when));
    }
}
