package com.orbitastra.backend.services.people.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.people.organization.Position;
import com.orbitastra.backend.repositories.people.organization.PositionRepository;

import lombok.RequiredArgsConstructor;

/**
 * The work {@link com.orbitastra.backend.services.people.OrganizationService} does around its
 * writes rather than inside them.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and a method here
 * never calls another method here.
 */
@Component
@RequiredArgsConstructor
public class OrganizationServiceUtils {

    private final PositionRepository positions;

    /**
     * Warns when a department's seats are all non-teaching.
     *
     * <p><b>A warning, not a refusal.</b> A department of entirely non-teaching seats is
     * legitimate — Finance, Facilities, Transport. What is not legitimate is a school discovering
     * months later that its teacher picker is empty because {@code teachingPosition} was left
     * false everywhere, and there was never an error to explain it.
     *
     * <p>Scoped to the department rather than the school, so creating Finance does not warn a
     * school whose Academics seats are correctly flagged.
     *
     * <p><b>One scoped query, not a scan.</b> This read every position of every school and
     * filtered in memory until 2026-09-15 — the only {@code findAll()} in the services layer. It
     * grew with the platform instead of with the department, and it pulled other tenants' rows
     * across a boundary they should never cross.
     *
     * @return the warning, or null when this seat teaches or some other seat in the unit does
     *
     * Used by:
     * - createPosition()
     */
    public String teachingWarning(School school, String departmentId, Position saved) {
        if (Boolean.TRUE.equals(saved.getTeachingPosition())) {
            return null;
        }

        // TODO: check position exists
        if (positions.existsBySchoolIdAndDepartmentDocsIdAndTeachingPositionIsTrue(
                school.getId(), departmentId)) {

            return null;
        }

        return "No seat in this department is marked teachingPosition. That is legitimate for a "
                + "unit like Finance — but it is also what an empty teacher picker looks like, "
                + "and nothing else will say so.";
    }
}
