package com.orbitastra.backend.services.crm.helper;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
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
     * The same cycle, and it has to be taking applications.
     *
     * <p><b>This is the module's replacement for gate 4.</b> Every other module refuses a write
     * against a year that is not running; admissions cannot, because a cycle for next year is the
     * normal case. What takes its place is the cycle's own status, and this is where that is
     * asked.
     *
     * <p>Does its own read rather than calling {@link #loadCycle}: a helper never calls another
     * helper, and one round trip either way.
     *
     * Used by:
     * - createApplication()
     */
    public AdmissionCycle loadOpenCycle(School school, String admissionCycleId) {
        String id = admissionCycleId == null ? "" : admissionCycleId.trim();

        // TODO: read admission cycle
        AdmissionCycle cycle = admissionCycles.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("ADMISSION_CYCLE_NOT_FOUND",
                        "No admission cycle with id '" + id + "' in this school."));

        if (cycle.getStatus() != AdmissionCycleStatus.OPEN) {
            throw ApiException.conflict("CYCLE_NOT_OPEN",
                    "'" + cycle.getName() + "' is " + cycle.getStatus() + ", so it is not taking "
                            + "applications. Only an OPEN cycle can be applied to — #3 is what "
                            + "opens one.");
        }
        return cycle;
    }
}
