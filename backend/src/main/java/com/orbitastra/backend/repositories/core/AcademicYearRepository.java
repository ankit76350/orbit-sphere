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
}
