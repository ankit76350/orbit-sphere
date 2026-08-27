package com.orbitastra.backend.repositories.plans;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.plans.SchoolSubscription;

public interface SchoolSubscriptionRepository extends MongoRepository<SchoolSubscription, String> {

    /**
     * The school's live subscription, if it has one.
     *
     * <p>{@code current} is uniquely indexed with a partial filter on {@code true}, so a school
     * can only ever have one — which is what makes this an Optional of one rather than a list to
     * pick through.
     */
    Optional<SchoolSubscription> findBySchoolIdAndCurrentIsTrue(String schoolId);
}
