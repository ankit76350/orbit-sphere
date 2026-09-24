package com.orbitastra.backend.repositories.crm.admissionoffer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.crm.admissionoffer.request.AdmissionOfferSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionOffer;
import com.orbitastra.backend.models.crm.enums.AdmissionOfferStatus;

import lombok.RequiredArgsConstructor;

/**
 * #32's query. Mirrors {@code AdmissionReviewRepositoryImpl}, which answers the same shape of
 * question about reviews.
 */
@RequiredArgsConstructor
public class AdmissionOfferRepositoryImpl implements AdmissionOfferRepositoryCustom {

    /**
     * The one status an offer can still lapse from.
     *
     * <p><b>A list of one, written as a list on purpose.</b> {@code DRAFT} would belong here if
     * anything could write it, and the day something does this is the line that changes rather
     * than an {@code is()} that has to become an {@code in()}.
     */
    private static final List<AdmissionOfferStatus> STILL_OUT = List.of(
            AdmissionOfferStatus.ISSUED);

    private final MongoTemplate mongo;

    @Override
    public Page<AdmissionOffer> search(String schoolId, AdmissionOfferSearchRequest request,
            Pageable pageable) {

        //! step 1 - the filter: the school, then whatever was asked for
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page, the same filter plus paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        // TODO: reading admission offers (how many match)
        long total = mongo.count(countQuery, AdmissionOffer.class);

        // TODO: reading admission offers (one page of them)
        List<AdmissionOffer> rows = mongo.find(pageQuery, AdmissionOffer.class);

        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, AdmissionOfferSearchRequest request) {

        //! step 1 - the school, always, and never from the caller. The tenant boundary.
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the one the index leads with. school_offer_status_expiry_idx is
        //! {schoolId, status, expiresAt}, which is this endpoint's whole reason for existing.
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }

        //! step 3 - one form's offer, for somebody walking down from an application.
        if (request.admissionApplicationDocsId() != null
                && !request.admissionApplicationDocsId().isBlank()) {
            filters.add(Criteria.where("admissionApplicationDocsId")
                    .is(request.admissionApplicationDocsId().trim()));
        }

        if (request.offeredClassDocsId() != null && !request.offeredClassDocsId().isBlank()) {
            filters.add(Criteria.where("offeredClassDocsId")
                    .is(request.offeredClassDocsId().trim()));
        }

        //! step 4 - what lapses before a date. The plain window question: "who do I ring this
        //! week". It says nothing about status, so it will also return offers already answered —
        //! which is why `expired` below exists as well.
        if (request.expiringBefore() != null) {
            filters.add(Criteria.where("expiresAt").lt(request.expiringBefore()));
        }

        //! step 5 - what has actually lapsed.
        //!
        //! TWO CONDITIONS, NOT ONE, exactly as #28's `overdue` is. A past date is not enough: an
        //! offer a family accepted last month has one too, and nobody needs chasing about it. So
        //! expired is "past its date AND still out", and `expired=false` is the mirror —
        //! everything that is not lapsed, including the answered ones and the ones with no date.
        //!
        //! AN OFFER WITH NO expiresAt IS NEVER EXPIRED. `$lt` does not match a missing field, so
        //! that falls out of the query rather than needing a rule — and it is the honest answer:
        //! an offer with no deadline is a seat held for ever, not one that has lapsed.
        if (request.expired() != null) {
            Instant now = Instant.now();
            Criteria lapsed = new Criteria().andOperator(
                    Criteria.where("expiresAt").lt(now),
                    Criteria.where("status").in(STILL_OUT));

            filters.add(request.expired() ? lapsed : new Criteria().norOperator(lapsed));
        }

        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
