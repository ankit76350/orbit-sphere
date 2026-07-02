package com.orbitastra.backend.repositories.core;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.core.School;

@Repository
public interface SchoolRepository extends MongoRepository<School, String> {
    Optional<School> findBySubdomain(String subdomain);

    List<School> findByActive(Boolean active);
}
