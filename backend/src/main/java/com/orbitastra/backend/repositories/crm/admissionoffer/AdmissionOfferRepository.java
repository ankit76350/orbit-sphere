package com.orbitastra.backend.repositories.crm.admissionoffer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.AdmissionOffer;

/**
 * Reads and writes for the {@code admission_offers} collection.
 *
 * <p><b>Nothing creates an offer yet.</b> #29 issues one, #30 answers it and #31 withdraws it;
 * none are built. Like the reviews, the read is written with #25 and returns an empty list until
 * they are.
 */
public interface AdmissionOfferRepository extends MongoRepository<AdmissionOffer, String>,
        AdmissionOfferRepositoryCustom {

    /**
     * One offer, for #30 and #31.
     *
     * <p><b>Scoped by {@code schoolId} in the query, never by id alone.</b> An id from another
     * school is a real id: answering or withdrawing against it would record this school's family
     * accepting another school's seat.
     */
    Optional<AdmissionOffer> findByIdAndSchoolId(String id, String schoolId);

    /**
     * Every offer made on one application, first revision first. For #25.
     *
     * <p><b>Scoped by school in the query.</b> Same reason as the reviews: the application id on
     * its own is not a tenant boundary.
     *
     * <p><b>Ordered by revision, which IS a total order</b> —
     * {@code school_application_offer_revision_uniq} makes the pair unique — so no second field is
     * needed here, unlike the reviews.
     *
     * <p><b>Every revision is returned, not just the live one.</b> A later offer supersedes the
     * one before it, and the superseded ones are the record of what the school offered first. An
     * endpoint that showed only the current offer would make "what did we originally offer this
     * family" unanswerable.
     */
    List<AdmissionOffer> findBySchoolIdAndAdmissionApplicationDocsIdOrderByRevisionNoAsc(
            String schoolId, String admissionApplicationDocsId);
}
