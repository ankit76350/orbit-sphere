package com.orbitastra.backend.repositories.facilities.facilityresource;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.facilities.FacilityResource;

/**
 * The school's rooms, halls, labs and other places — a paper's exam room is one of them.
 *
 * <p>Every query carries {@code schoolId}, so another school's room is never found.
 */
public interface FacilityResourceRepository extends MongoRepository<FacilityResource, String> {

    /** One room of this school, by its id. */
    Optional<FacilityResource> findByIdAndSchoolId(String id, String schoolId);
}
