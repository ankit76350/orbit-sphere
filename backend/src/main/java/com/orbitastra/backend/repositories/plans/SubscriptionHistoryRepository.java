package com.orbitastra.backend.repositories.plans;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.plans.SubscriptionHistory;

/**
 * The record of everything that ever happened to a subscription.
 *
 * <p><b>Append-only by convention, and it has to stay that way.</b> A history row exists so that
 * "why is this school suspended" has an answer months later. Nothing may update or delete one —
 * the interface deliberately offers no way to, beyond what {@code MongoRepository} brings, and
 * the day this matters enough it should be narrowed to inserts the way {@code AuditEvent} asks
 * for.
 */
public interface SubscriptionHistoryRepository extends MongoRepository<SubscriptionHistory, String> {

    /** One subscription's story, newest first. */
    List<SubscriptionHistory> findBySchoolIdAndSchoolSubscriptionDocsIdOrderByEffectiveAtDesc(
            String schoolId, String schoolSubscriptionDocsId);
}
