package com.orbitastra.backend.repositories.crm.admissioncycle;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.AdmissionCycle;

/**
 * Reads and writes for the {@code admission_cycles} collection.
 *
 * <p>Only what #1 and #5 need so far. The rest get added when those endpoints are built, so this
 * file always says what is actually used.
 *
 * <p>The search #5 runs is in {@link AdmissionCycleRepositoryCustom}: every filter on it is
 * optional, so the query has to be built at runtime rather than declared as a method name.
 */
public interface AdmissionCycleRepository
        extends MongoRepository<AdmissionCycle, String>, AdmissionCycleRepositoryCustom {

    /**
     * Is a cycle with this name already set up for this year in this school?
     *
     * <p>The database also stops it with the {@code school_academic_year_cycle_name_uniq} index.
     * We ask first so the caller gets a clear message that names the cycle, instead of a duplicate
     * key error.
     */
    boolean existsBySchoolIdAndAcademicYearAndName(String schoolId, String academicYear,
            String name);
}
