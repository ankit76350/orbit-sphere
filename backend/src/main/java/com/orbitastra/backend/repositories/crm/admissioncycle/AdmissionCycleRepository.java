package com.orbitastra.backend.repositories.crm.admissioncycle;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.AdmissionCycle;

/**
 * Reads and writes for the {@code admission_cycles} collection.
 *
 * <p>Only what #1, #5 and #6 need so far. The rest get added when those endpoints are built, so this
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

    /**
     * One cycle, for #6.
     *
     * <p><b>Scoped by schoolId in the query, never by id alone.</b> An id from another school is a
     * real id: looking it up without the school would find it and hand it over. This is the
     * difference between "not found" and a tenant leak, and it is one argument.
     */
    Optional<AdmissionCycle> findByIdAndSchoolId(String id, String schoolId);
}
