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
 * Central check that a date belongs to an academic year.
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
 * <p><b>THE RULE IS THE END DATE, AND ONLY THE END DATE — changed 2026-09-28.</b> A date belongs
 * to a year if it is not after that year ends. There is no lower bound.
 *
 * <p>This component was written with one, and then with a per-field exemption for the dates that
 * were allowed to run early, and both were wrong in the same direction. <b>A school plans a year
 * before it starts.</b> Admissions for 2026-2027 open, close and take their enrolment deadline in
 * the months running up to it, and every one of those dates is correct. Which fields those are is
 * not something a shared component can know, and the exemption set that tried to say so ended up
 * naming all of them.
 *
 * <p><b>The half that is kept is the half that catches mistakes.</b> A date before the year is a
 * school planning ahead; a date after it is a typo, and a deadline in 2099 is the one this refuses.
 * The cost of dropping the lower bound is recorded honestly: a cycle for 2026-2027 whose four dates
 * are all in 2019 is now accepted, because nothing here can tell that from a school working a long
 * way ahead. A floor — nothing more than a year before the year starts — is what would catch it,
 * and it is not here because no caller has asked for one.
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
 * <p><b>A year with no end date is not checked.</b> It is {@code @NotNull} on the model, but
 * nothing enforces that on save, and refusing every date because a year predates the rule would
 * break the schools least able to fix it.
 *
 * <p><b>400, not 409.</b> A date after the year ends is a value the caller sent, not a state the
 * school is in — which is the line the rest of this project draws between the two.
 *
 * <p><b>Two methods, and it has had seven.</b> Three convenience overloads went on 2026-09-28
 * because nothing called them, and an {@code allInside} that answered the question without the
 * refusal went the same day once #3 started refusing rather than deciding. An unused method whose
 * behaviour is untested is where a wrong answer hides. A module that wants another form adds it
 * then, with a test behind it.
 */
@Component
@RequiredArgsConstructor
public class AcademicYearWindow {

    private final AcademicYearRepository academicYears;

    /**
     * Refuses any of {@code dates} that falls after the named year ends.
     *
     * <p>The load-by-name form of the overload below, for callers that have only the name — which
     * is most of them, because a year's name is what other collections store.
     *
     * <p>Used by:
     * - AdmissionCycleService.createCycle()
     * - AdmissionCycleService.updateCycle()
     * - AdmissionCycleService.moveStatus()
     *
     * @throws ApiException {@code 404 ACADEMIC_YEAR_NOT_FOUND} when the school has no such year
     */
    public void requireNotAfterYearEnd(School school, String academicYearName, SchoolTimeZone zone,
            String errorCode, Map<String, Instant> dates) {

        //! step 1 - the year, which has to be one this school actually has
        // TODO: read academic year
        AcademicYear year = academicYears
                .findBySchoolIdAndName(school.getId(), academicYearName)
                .orElseThrow(() -> ApiException.notFound("ACADEMIC_YEAR_NOT_FOUND",
                        "No academic year called '" + academicYearName + "' in this school."));

        //! step 2 - the dates
        requireNotAfterYearEnd(year, zone, errorCode, dates);
    }

    /**
     * Refuses any of {@code dates} that falls after this academic year ends.
     *
     * <p><b>One pass, in the map's order</b>, so the first offending field is the one reported —
     * a caller who moved four dates by a year wants to be told once.
     *
     * <p><b>An absent date is not an offence.</b> A map entry with no value is a field the caller
     * did not set, and a check is not the place to decide whether it had to be.
     */
    public void requireNotAfterYearEnd(AcademicYear year, SchoolTimeZone zone, String errorCode,
            Map<String, Instant> dates) {

        //! step 3 - the first one past the end, and nothing after that.
        String offender = firstAfterYearEnd(year, zone, dates);
        if (offender == null) {
            return;
        }

        throw ApiException.badRequest(errorCode,
                offender + " is " + Dates.readable(dates.get(offender), zone)
                        + ", which is after '" + year.getName() + "' ends. That year ends "
                        + Dates.readable(year.getEndDate()) + ".");
    }

    /**
     * The first field that falls after the year ends, or {@code null}.
     *
     * <p><b>Private, so it is not one of this component's methods calling another.</b> That rule
     * is about the surface callers use; this is the file's own arithmetic, kept in one place so
     * that the two {@code require} forms can never disagree about where a year stops.
     */
    private String firstAfterYearEnd(AcademicYear year, SchoolTimeZone zone,
            Map<String, Instant> dates) {

        if (year.getEndDate() == null) {
            return null;
        }

        //! THE DAY AFTER, AT MIDNIGHT, IN THE SCHOOL'S ZONE. Anything strictly before that instant
        //! happened on or before the last day of the year.
        Instant closes = year.getEndDate().plusDays(1).atStartOfDay(zone.toZoneId()).toInstant();

        for (Map.Entry<String, Instant> each : dates.entrySet()) {
            Instant when = each.getValue();
            if (when != null && !when.isBefore(closes)) {
                return each.getKey();
            }
        }
        return null;
    }

}
