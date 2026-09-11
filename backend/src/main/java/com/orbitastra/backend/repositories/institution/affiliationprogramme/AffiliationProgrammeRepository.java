package com.orbitastra.backend.repositories.institution.affiliationprogramme;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.institution.AffiliationProgramme;

/**
 * Affiliation programmes, so a class can say which board's programme it runs under.
 *
 * <p><b>Created 2026-09-10 for endpoint #12</b>, which accepts an
 * {@code affiliationProgrammeDocsId}. Until this existed there was no way to check that the id
 * was real or that it belonged to the caller's school, so the field would have accepted another
 * tenant's programme id — the exact bug {@code SchoolBase} exists to prevent, discovered when a
 * report printed the wrong board.
 *
 * <p>In its own {@code affiliationprogramme/} folder rather than loose in
 * {@code repositories/institution/}. The two files already sitting loose in that module predate
 * the rule and are a known open point; do not copy them.
 */
public interface AffiliationProgrammeRepository
        extends MongoRepository<AffiliationProgramme, String> {

    /**
     * One programme, scoped to its school.
     *
     * <p>{@code schoolId} is in the query even though the id is globally unique. Looking up by id
     * alone would find another school's programme and quietly accept it.
     */
    Optional<AffiliationProgramme> findByIdAndSchoolId(String id, String schoolId);
}
