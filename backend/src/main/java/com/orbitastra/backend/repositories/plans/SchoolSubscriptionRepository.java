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

    /**
     * How many schools are on one plan version.
     *
     * <p>Backed by {@code subscription_plan_version_idx}. A plan version is its own document, so
     * its id alone identifies the version — the stored {@code planVersion} is a convenience for
     * reading a subscription, not part of the link.
     *
     * <p>Counts <b>every</b> subscription pointing at it, including cancelled and expired ones.
     * That is deliberate for the version history: "nobody is on it now" and "nobody ever was"
     * are different answers to "can this version be retired", and the second is the one that
     * says the version can be forgotten.
     */
    long countByPlanDefinitionDocsId(String planDefinitionDocsId);
}
