package com.orbitastra.backend.repositories.crm.admissionreview;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.crm.admissionreview.request.AdmissionReviewSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionReview;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdmissionReviewRepositoryImpl implements AdmissionReviewRepositoryCustom {

    /**
     * The statuses an overdue review can be in.
     *
     * <p><b>A finished review is never overdue</b>, however long ago its due date was. "Overdue"
     * means somebody still owes the work — a review completed a week late is late, not outstanding,
     * and putting it on a queue of things to do would be wrong twice over.
     */
    private static final List<AdmissionReviewStatus> STILL_OWED = List.of(
            AdmissionReviewStatus.PENDING, AdmissionReviewStatus.IN_PROGRESS);

    private final MongoTemplate mongo;

    @Override
    public Page<AdmissionReview> search(String schoolId, AdmissionReviewSearchRequest request,
            Pageable pageable) {

        //! step 1 - the filter: the school, then whatever was asked for
        Criteria criteria = buildCriteria(schoolId, request);

        //! step 2 - the count, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page, the same filter plus paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        // TODO: reading admission reviews (how many match)
        long total = mongo.count(countQuery, AdmissionReview.class);

        // TODO: reading admission reviews (one page of them)
        List<AdmissionReview> rows = mongo.find(pageQuery, AdmissionReview.class);

        return new PageImpl<>(rows, pageable, total);
    }

    private Criteria buildCriteria(String schoolId, AdmissionReviewSearchRequest request) {

        //! step 1 - the school, always, and never from the caller. The tenant boundary.
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - the two the index is built for, in its order: reviewer, then status.
        //! school_reviewer_status_due_idx is {schoolId, reviewerDocsId, status, dueAt}, which is
        //! this endpoint's whole reason for existing — one person's outstanding work, soonest
        //! first.
        if (request.reviewerDocsId() != null && !request.reviewerDocsId().isBlank()) {
            filters.add(Criteria.where("reviewerDocsId").is(request.reviewerDocsId().trim()));
        }
        if (request.status() != null) {
            filters.add(Criteria.where("status").is(request.status()));
        }

        //! step 3 - one form's reviews, for somebody walking down from an application rather than
        //! across from a person.
        if (request.admissionApplicationDocsId() != null
                && !request.admissionApplicationDocsId().isBlank()) {
            filters.add(Criteria.where("admissionApplicationDocsId")
                    .is(request.admissionApplicationDocsId().trim()));
        }

        if (request.reviewRound() != null) {
            filters.add(Criteria.where("reviewRound").is(request.reviewRound()));
        }

        //! step 4 - what is late.
        //!
        //! TWO CONDITIONS, NOT ONE. A past due date is not enough: a review somebody finished last
        //! month also has one, and it is not owed. So overdue is "past its date AND still to be
        //! done", and `overdue=false` is the mirror — everything that is not late, including the
        //! finished ones and the ones with no date at all.
        //!
        //! A REVIEW WITH NO dueAt IS NEVER OVERDUE. `$lt` does not match a missing field, so that
        //! falls out of the query rather than needing a rule.
        if (request.overdue() != null) {
            Instant now = Instant.now();
            Criteria late = new Criteria().andOperator(
                    Criteria.where("dueAt").lt(now),
                    Criteria.where("status").in(STILL_OWED));

            filters.add(request.overdue() ? late : new Criteria().norOperator(late));
        }

        return new Criteria().andOperator(filters.toArray(new Criteria[0]));
    }
}
