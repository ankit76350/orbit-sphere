package com.orbitastra.backend.repositories.plans.schoolsubscription;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.plans.SchoolSubscription;

public interface SchoolSubscriptionRepository extends MongoRepository<SchoolSubscription, String>,
        SchoolSubscriptionRepositoryCustom {

    /**
     * The school's live subscription, if it has one.
     *
     * <p>{@code current} is uniquely indexed with a partial filter on {@code true}, so a school
     * can only ever have one — which is what makes this an Optional of one rather than a list to
     * pick through.
     */
    Optional<SchoolSubscription> findBySchoolIdAndCurrentIsTrue(String schoolId);

    /**
     * One school's subscription, by the number printed on it.
     *
     * <p>The school id is in the query as well as the number, so a caller who guesses another
     * school's subscription number gets nothing back rather than somebody else's record.
     * Subscription numbers are only unique within a school, so this pair is what identifies one.
     */
    Optional<SchoolSubscription> findBySchoolIdAndSubscriptionNo(String schoolId,
            String subscriptionNo);

    /**
     * One school's subscription, by its own document id.
     *
     * <p><b>The school id is in the query even though the document id is globally unique.</b>
     * That is not redundancy, it is the tenant boundary: a caller who guesses or is handed
     * another school's subscription id gets nothing back rather than somebody else's record —
     * which matters most on #29, where what comes back is an audit trail.
     *
     * <p>Needed because #28 returns {@code subscriptionId} and #29 accepts it in the path: a
     * subscription number has slashes in it and cannot be written in a URL.
     */
    Optional<SchoolSubscription> findBySchoolIdAndId(String schoolId, String id);

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
