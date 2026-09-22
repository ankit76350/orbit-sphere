package com.orbitastra.backend.services.crm.helper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.common.time.SchoolZone;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;
import com.orbitastra.backend.repositories.crm.admissioncycle.AdmissionCycleRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the CRM services share. One file for the module, per this project's folder rules.
 *
 * <p>It arrived when {@code AdmissionApplicationService} became the module's second service and
 * needed to load a cycle, which {@code AdmissionCycleService} was already doing in four places.
 * Five copies of one lookup is five places for the tenant scoping to be got wrong once.
 *
 * <p><b>Helper methods never call each other.</b> Each stands on its own, which is why
 * {@link #loadOpenCycle} repeats the read rather than calling {@link #loadCycle}.
 */
@Component
@RequiredArgsConstructor
public class CrmHelper {

    private final AdmissionCycleRepository admissionCycles;
    private final SchoolZone schoolZone;

    /**
     * One admission cycle of this school, or a 404.
     *
     * <p><b>Scoped by school in the query, never checked afterwards.</b> An id from another school
     * is a real id: finding it first and testing the school after would already have read it, and
     * a "not found" that depends on remembering to check is one refactor from a leak.
     *
     * Used by:
     * - getCycle()
     * - moveStatus()
     * - setCapacities()
     * - updateCycle()
     */
    public AdmissionCycle loadCycle(School school, String admissionCycleId) {
        String id = admissionCycleId == null ? "" : admissionCycleId.trim();

        // TODO: read admission cycle
        return admissionCycles.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("ADMISSION_CYCLE_NOT_FOUND",
                        "No admission cycle with id '" + id + "' in this school."));
    }

    /**
     * The same cycle, and it has to be taking applications <b>right now</b>.
     *
     * <h2>Two different questions, and both have to be asked</h2>
     *
     * <p><b>The status</b> is the module's replacement for gate 4. Every other module refuses a
     * write against a year that is not running; admissions cannot, because a cycle for next year
     * is the normal case. The cycle's own status takes its place.
     *
     * <p><b>The published window</b> is the second, and the status cannot stand in for it. A
     * school publishes "applications close 31 August", forgets to move the cycle to CLOSED on the
     * 1st, and forms keep arriving against a round the families were told had shut. The status
     * says nobody pressed a button; the date says what the school promised.
     *
     * <p><b>A date that is absent constrains nothing.</b> Most cycles carry no calendar at all,
     * and an absent date is "the school did not publish one", not "midnight".
     *
     * <p>Does its own read rather than calling {@link #loadCycle}: a helper never calls another
     * helper, and it is one round trip either way.
     *
     * <p><b>Both callers ask at the moment the thing happens</b>, not at the moment the form was
     * started: #17 when the family begins, #19 when they send it. A draft begun an hour before
     * the deadline and submitted an hour after it is a late application, and the window is there
     * to catch exactly that.
     *
     * Used by:
     * - createApplication()
     * - submitApplication()
     */
    public AdmissionCycle loadOpenCycle(School school, String admissionCycleId) {
        String id = admissionCycleId == null ? "" : admissionCycleId.trim();

        // TODO: read admission cycle
        AdmissionCycle cycle = admissionCycles.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("ADMISSION_CYCLE_NOT_FOUND",
                        "No admission cycle with id '" + id + "' in this school."));

        //! step 1 - the switch the office controls
        if (cycle.getStatus() != AdmissionCycleStatus.OPEN) {
            throw ApiException.conflict("CYCLE_NOT_OPEN",
                    "'" + cycle.getName() + "' is " + cycle.getStatus() + ", so it is not taking "
                            + "applications. Only an OPEN cycle can be applied to or submitted "
                            + "into — #3 is what opens one.");
        }

        //! step 2 - the calendar the school published. Checked even though the cycle is OPEN,
        //! because the two can disagree: opening early leaves the start date in the future, and
        //! forgetting to close leaves the end date in the past while forms still arrive.
        Instant now = Instant.now();
        SchoolTimeZone zone = schoolZone.of(school);

        if (cycle.getApplicationOpenAt() != null && now.isBefore(cycle.getApplicationOpenAt())) {
            throw ApiException.conflict("APPLICATIONS_NOT_OPEN_YET",
                    "'" + cycle.getName() + "' does not take applications until "
                            + Dates.readable(cycle.getApplicationOpenAt(), zone)
                            + ". The cycle is OPEN, but the date the school published has not "
                            + "arrived — move that date or wait for it.");
        }

        if (cycle.getApplicationCloseAt() != null && now.isAfter(cycle.getApplicationCloseAt())) {
            throw ApiException.conflict("APPLICATIONS_CLOSED",
                    "'" + cycle.getName() + "' stopped taking applications on "
                            + Dates.readable(cycle.getApplicationCloseAt(), zone)
                            + ". The cycle is still marked OPEN — somebody has not closed it — "
                            + "but the date the school published has passed.");
        }

        return cycle;
    }
}
