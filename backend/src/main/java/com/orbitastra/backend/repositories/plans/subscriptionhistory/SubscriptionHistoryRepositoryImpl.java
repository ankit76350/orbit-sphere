package com.orbitastra.backend.repositories.plans.subscriptionhistory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionHistorySearchRequest;
import com.orbitastra.backend.models.plans.SubscriptionHistory;

import lombok.RequiredArgsConstructor;

/**
 * The dynamic audit-trail query behind #29.
 *
 * <p><b>Everything happens in the database.</b> Filtering, sorting and paging are all on the
 * query, so one page of rows is read however long the trail is. The same arrangement as
 * {@code SchoolSubscriptionRepositoryImpl}.
 *
 * <p>Two round trips: one for the page, one for the total behind {@code totalElements}. The count
 * carries the filter and nothing else — given the page's skip and limit it would only ever count
 * one page — and it is built from the same {@link Criteria} object rather than a second
 * hand-written copy, which is the only way the two cannot drift apart.
 *
 * <h2>The index behind it</h2>
 *
 * <p>{@code school_subscription_event_time_idx} on
 * {@code {schoolId: 1, schoolSubscriptionDocsId: 1, effectiveAt: -1, createdAt: -1}} was already
 * declared on the document and serves this exactly: the first two keys are the equality match
 * every request makes, and the last two are the default order, in the same direction. <b>No new
 * index was added for #29</b>, and measuring says none is wanted — with that index built, the
 * default query reads 11 documents to return 11:
 *
 * <pre>
 * without it   COLLSCAN, 4189 documents examined, 11 returned
 * with it      IXSCAN,     11 documents examined, 11 returned
 * </pre>
 *
 * <p><b>But it is not built in the dev database</b>, where the collection has only {@code _id_}.
 * That is deliberate rather than an oversight: {@code spring.data.mongodb.auto-index-creation} is
 * off because turning it on cost six minutes on every boot, so indexes are built on demand with
 * {@code app.mongo.sync-indexes=true}. Until that has been run against an environment, this
 * endpoint collection-scans there. It is a deployment step, not a code change.
 *
 * <p>The trailing {@code _id} in the sort does not spoil it — see the note on the service.
 */
@RequiredArgsConstructor
public class SubscriptionHistoryRepositoryImpl implements SubscriptionHistoryRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<SubscriptionHistory> search(String schoolId, String schoolSubscriptionDocsId,
            SubscriptionHistorySearchRequest request, Pageable pageable) {

        //! step 1 - build the filter: the boundary, then whichever parameters were sent
        Criteria criteria = buildCriteria(schoolId, schoolSubscriptionDocsId, request);

        //! step 2 - the count query, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page query, the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading subscription history (how many match)
        long total = mongo.count(countQuery, SubscriptionHistory.class);

        //! step 5 - run the page. Only these rows are read, however long the trail is.
        // TODO: reading subscription history (one page of it)
        List<SubscriptionHistory> rows = mongo.find(pageQuery, SubscriptionHistory.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * The filters, combined with AND. An absent one adds nothing, so no filters means the whole
     * trail of this one subscription.
     *
     * <p><b>The school and the subscription are not optional and are not filters.</b> They go on
     * first and always: a bug that dropped either would answer one subscription's request with
     * another's audit rows, which is the one failure mode an audit endpoint must not have.
     */
    private Criteria buildCriteria(String schoolId, String schoolSubscriptionDocsId,
            SubscriptionHistorySearchRequest request) {

        //! step 1 - the boundary, first and unconditionally: the tenant, then the subscription
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));
        filters.add(Criteria.where("schoolSubscriptionDocsId").is(schoolSubscriptionDocsId));

        //! step 2 - what happened, which ORs within itself
        if (request.eventTypes() != null && !request.eventTypes().isEmpty()) {
            filters.add(Criteria.where("eventType").in(request.eventTypes()));
        }

        //! step 3 - the two ends of the transition, each ORing within itself. Separate fields, so
        //! sending both asks for a specific move — TRIAL to ACTIVE and nothing else.
        if (request.newStatuses() != null && !request.newStatuses().isEmpty()) {
            filters.add(Criteria.where("newStatus").in(request.newStatuses()));
        }
        if (request.previousStatuses() != null && !request.previousStatuses().isEmpty()) {
            filters.add(Criteria.where("previousStatus").in(request.previousStatuses()));
        }

        //! step 4 - where the change came from. Case-insensitive and anchored at both ends, so it
        //! is an exact match that forgives the shift key rather than a substring: `admin_portal`
        //! finds ADMIN_PORTAL, and a source of `PORTAL` does not.
        if (request.source() != null && !request.source().isBlank()) {
            filters.add(Criteria.where("source")
                    .regex("^" + Pattern.quote(request.source().trim()) + "$", "i"));
        }

        //! step 5 - the two exact ids: who acted, and what outside event caused it
        if (request.performedByDocsId() != null && !request.performedByDocsId().isBlank()) {
            filters.add(Criteria.where("performedByDocsId").is(request.performedByDocsId().trim()));
        }
        if (request.sourceEventId() != null && !request.sourceEventId().isBlank()) {
            filters.add(Criteria.where("sourceEventId").is(request.sourceEventId().trim()));
        }

        //! step 6 - the reason, as a substring anywhere in it. QUOTED before it reaches the
        //! query: a reason typed as `.*` or `(` is what an operator wrote, not a pattern, and
        //! passing it through raw would either match every row or throw on an unclosed bracket.
        if (request.reason() != null && !request.reason().isBlank()) {
            filters.add(Criteria.where("reason")
                    .regex(Pattern.quote(request.reason().trim()), "i"));
        }

        //! step 7 - the two date windows. Each end is INCLUSIVE (gte / lte), because a caller
        //! filtering "from 1 April" means a change that took effect on 1 April is in, and an
        //! exclusive bound would silently drop the row they were looking for.
        //!
        //! The two ends of one window go on the SAME criteria object rather than as two separate
        //! `where` clauses: two clauses on one field is a document with two keys of the same
        //! name, and Mongo keeps only the last — so a from/to pair would filter on `to` alone
        //! and quietly return too much.
        if (request.effectiveFrom() != null || request.effectiveTo() != null) {
            Criteria effective = Criteria.where("effectiveAt");
            if (request.effectiveFrom() != null) {
                effective = effective.gte(request.effectiveFrom());
            }
            if (request.effectiveTo() != null) {
                effective = effective.lte(request.effectiveTo());
            }
            filters.add(effective);
        }

        if (request.recordedFrom() != null || request.recordedTo() != null) {
            Criteria recorded = Criteria.where("createdAt");
            if (request.recordedFrom() != null) {
                recorded = recorded.gte(request.recordedFrom());
            }
            if (request.recordedTo() != null) {
                recorded = recorded.lte(request.recordedTo());
            }
            filters.add(recorded);
        }

        //! step 8 - AND them together. The boundary alone is the no-filter case.
        return new Criteria().andOperator(filters);
    }
}
