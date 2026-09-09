package com.orbitastra.backend.repositories.core;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.core.School;

/**
 * The tenant root. Deliberately not school-scoped — this is the collection that defines a
 * school, so there is no schoolId to filter on.
 */
public interface SchoolRepository extends MongoRepository<School, String>, SchoolRepositoryCustom {
    // SchoolRepository
    //         ↓
    // SchoolRepositoryCustom
    //         ↓
    // SchoolRepositoryImpl

    Optional<School> findBySubdomain(String subdomain);

    boolean existsBySubdomain(String subdomain);
}
