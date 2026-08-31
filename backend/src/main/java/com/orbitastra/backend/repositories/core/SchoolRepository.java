package com.orbitastra.backend.repositories.core;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.core.School;

/**
 * The tenant root. Deliberately not school-scoped — this is the collection that defines a
 * school, so there is no schoolId to filter on.
 */
public interface SchoolRepository extends MongoRepository<School, String> {

    Optional<School> findBySubdomain(String subdomain);

    boolean existsBySubdomain(String subdomain);

    /**
     * Who used to answer to this label, if anyone.
     *
     * <p>A released subdomain stays reserved to the school that gave it up — see
     * {@code School.previousSubdomains}. This is how #10 tells "nobody has this" from "this is
     * somebody's old address", which are very different answers to the same request.
     */
    Optional<School> findByPreviousSubdomainsContaining(String subdomain);
}
