package com.orbitastra.backend.repositories.institution;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.institution.NumberSequence;

public interface NumberSequenceRepository extends MongoRepository<NumberSequence, String> {

    List<NumberSequence> findBySchoolId(String schoolId);

    boolean existsBySchoolId(String schoolId);

    /** For the activation readiness check — counting in the database beats loading 47 rows. */
    long countBySchoolId(String schoolId);
}
