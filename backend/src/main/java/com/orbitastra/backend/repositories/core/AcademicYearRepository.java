package com.orbitastra.backend.repositories.core;

import java.time.LocalDate;

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
}
