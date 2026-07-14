package com.orbitastra.backend.services.utils;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;

import lombok.RequiredArgsConstructor;

/**
 * Single source of truth for scoping data to a school's academic year.
 *
 * SaaS tenancy model: school -> academic year -> year-scoped data. Records
 * reference the year by its {@code name} (unique per school, e.g. "2026-2027"),
 * never by the Mongo document id. Every service that stores a year-scoped
 * record resolves the year through this component so the rules stay identical
 * everywhere.
 */
@Component
@RequiredArgsConstructor
public class AcademicYearResolver {

    private final AcademicYearRepository academicYearRepository;

    /** Validates that a year with this name exists for the school and returns it. */
    public AcademicYear byName(String schoolId, String name) {
        return academicYearRepository.findBySchoolIdAndName(schoolId, name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic year '" + name + "' not found for this school."));
    }

    /** Returns the school's academic year whose start/end dates contain the given date. */
    public AcademicYear byDate(String schoolId, LocalDate date) {
        return academicYearRepository.findBySchoolId(schoolId).stream()
                .filter(y -> y.getStartDate() != null && y.getEndDate() != null
                        && !date.isBefore(y.getStartDate()) && !date.isAfter(y.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The date " + date
                        + " does not fall in any academic year of this school. "
                        + "Create the academic year (e.g. '2026-2027') with its start/end dates first."));
    }

    /**
     * Resolves the academic year for a record:
     * <ul>
     *   <li>if {@code providedName} is given, validate it exists — and, when a
     *       {@code fallbackDate} is also given, that the date falls inside it;</li>
     *   <li>else derive the year from {@code fallbackDate};</li>
     *   <li>else fail, asking the caller to supply the year.</li>
     * </ul>
     * Callers store {@link AcademicYear#getName()} on their record.
     */
    public AcademicYear resolve(String schoolId, String providedName, LocalDate fallbackDate) {
        if (providedName != null && !providedName.isBlank()) {
            AcademicYear year = byName(schoolId, providedName);
            if (fallbackDate != null
                    && (fallbackDate.isBefore(year.getStartDate()) || fallbackDate.isAfter(year.getEndDate()))) {
                throw new IllegalArgumentException("The date " + fallbackDate
                        + " is outside academic year '" + year.getName() + "', which runs "
                        + year.getStartDate() + " to " + year.getEndDate() + ".");
            }
            return year;
        }
        if (fallbackDate != null) {
            return byDate(schoolId, fallbackDate);
        }
        throw new IllegalArgumentException(
                "Please specify the academic year (e.g. '2026-2027') for this record.");
    }

    /**
     * Enforces that the academic year of an existing record is not changed on
     * update — moving a record between years would corrupt year-scoped reports.
     */
    public void assertImmutable(String existing, String incoming) {
        if (incoming != null && !incoming.isBlank() && existing != null && !incoming.equals(existing)) {
            throw new IllegalArgumentException("The academic year of a record cannot be changed once set ('"
                    + existing + "'). Create the record under the correct academic year instead.");
        }
    }
}
