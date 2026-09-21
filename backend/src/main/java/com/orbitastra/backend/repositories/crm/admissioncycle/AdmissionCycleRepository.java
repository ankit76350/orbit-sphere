package com.orbitastra.backend.repositories.crm.admissioncycle;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.AdmissionCycle;

/**
 * Reads and writes for the {@code admission_cycles} collection.
 *
 * <p>Only what endpoint #1 needs so far. The finder methods for #5 and #6 get added when those
 * endpoints are built, so this file always says what is actually used.
 */
public interface AdmissionCycleRepository extends MongoRepository<AdmissionCycle, String> {

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
