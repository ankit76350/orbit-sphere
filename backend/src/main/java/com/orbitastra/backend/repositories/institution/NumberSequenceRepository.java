package com.orbitastra.backend.repositories.institution;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.institution.NumberSequence;

/**
 * One document per school holds all of that school's counters, so every method here is keyed on
 * schoolId alone and returns at most one document. It was one document per counter until
 * 2026-09-05 — see NumberSequence.
 *
 * <p><b>Allocation is on the custom fragment</b>, not here: it has to be an atomic
 * {@code findAndModify} with {@code $inc} on the matched array element, which no method name can
 * express. See {@link NumberSequenceRepositoryCustom}. Reading a document, adding one in Java and
 * saving it back is how two students get the same admission number.
 */
public interface NumberSequenceRepository
        extends MongoRepository<NumberSequence, String>, NumberSequenceRepositoryCustom {
    // NumberSequenceRepository
    //         ↓
    // NumberSequenceRepositoryCustom
    //         ↓
    // NumberSequenceRepositoryImpl


    /** The school's counters document, or empty when it has never been provisioned. */
    Optional<NumberSequence> findBySchoolId(String schoolId);

    boolean existsBySchoolId(String schoolId);
}
