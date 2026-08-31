package com.orbitastra.backend.repositories.core;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.core.AcademicYear;

public interface AcademicYearRepository extends MongoRepository<AcademicYear, String> {

    /**
     * Is a year running on this date?
     *
     * <p>Used by the time-zone change guard. There is no {@code current} flag on AcademicYear —
     * deliberately — so "is a year in progress" is answered from the dates, which are the only
     * authority on it.
     */
    boolean existsBySchoolIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String schoolId, LocalDate onOrBefore, LocalDate onOrAfter);

    /**
     * Looked up by name, not by id, because that is how the whole system refers to a year:
     * every other collection stores {@code academicYear} as this string.
     */
    Optional<AcademicYear> findBySchoolIdAndName(String schoolId, String name);

    boolean existsBySchoolIdAndName(String schoolId, String name);

    /** All of a school's years. Small — a handful of rows — and needed for the overlap check. */
    List<AcademicYear> findBySchoolId(String schoolId);

    /**
     * All of a school's years, newest first. Used by G5.
     *
     * <p>Sorted on startDate rather than createdAt, because "newest" means the year furthest
     * along the calendar, not the row that happened to be typed in last. A school setting up
     * enters 2024-2025 after 2025-2026 often enough that the two orders disagree.
     *
     * <p>Sorted in the database rather than in Java. The list is small enough that it would not
     * matter today, but a sort written in the service is a sort the next list endpoint has to
     * remember to copy.
     */
    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(String schoolId);
}
