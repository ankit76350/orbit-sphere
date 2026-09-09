package com.orbitastra.backend.repositories.plans.schoolsubscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionSearchRequest;
import com.orbitastra.backend.models.plans.SchoolSubscription;

import lombok.RequiredArgsConstructor;

/**
 * The dynamic subscription query behind #28.
 *
 * <p><b>Everything happens in the database.</b> Filtering, sorting and paging are all on the
 * query, so one page of documents is read however many periods a school has been through. The
 * same arrangement as the plan catalogue and the school list.
 *
 * <p>Two round trips: one for the page, one for the total behind {@code totalElements}. The count
 * carries the filter and nothing else — given the page's skip and limit it would only ever count
 * one page — and it is built from the same {@link Criteria} object rather than a second
 * hand-written copy, which is the only way the two cannot drift apart.
 *
 * <h2>The index behind it</h2>
 *
 * <p>{@code school_subscription_status_period_idx} on
 * {@code {schoolId: 1, status: 1, currentPeriodEnd: 1}} already serves this. Mongo can use an
 * index prefix, so a bare school-scoped query uses its first key and a
 * {@code ?status=} filter uses the first two. No new index was added for #28 — see the note on
 * the service, which explains why the sort does not want one either.
 */
@RequiredArgsConstructor
public class SchoolSubscriptionRepositoryImpl implements SchoolSubscriptionRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<SchoolSubscription> search(String schoolId, SubscriptionSearchRequest request,
            Collection<String> planDefinitionDocsIds, Pageable pageable) {

        //! step 1 - build the filter: the tenant, then whichever parameters were sent
        Criteria criteria = buildCriteria(schoolId, request, planDefinitionDocsIds);

        //! step 2 - the count query, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page query, the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading subscriptions (how many match)
        long total = mongo.count(countQuery, SchoolSubscription.class);

        //! step 5 - run the page. Only these rows are read, however long the history is.
        // TODO: reading subscriptions (one page of them)
        List<SchoolSubscription> rows = mongo.find(pageQuery, SchoolSubscription.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * The filters, combined with AND. An absent one adds nothing, so no filters means the whole
     * history of this one school.
     *
     * <p><b>The school id is not optional and is not a filter.</b> It goes on first and always,
     * because it is the tenant boundary: a bug that dropped it would answer one school's request
     * with another's records, which is the one failure mode this endpoint must not have.
     */
    private Criteria buildCriteria(String schoolId, SubscriptionSearchRequest request,
            Collection<String> planDefinitionDocsIds) {

        //! step 1 - the tenant, first and unconditionally
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("schoolId").is(schoolId));

        //! step 2 - status, which ORs within itself
        if (request.statuses() != null && !request.statuses().isEmpty()) {
            filters.add(Criteria.where("status").in(request.statuses()));
        }

        //! step 3 - cadence, which ORs within itself too
        if (request.billingCycles() != null && !request.billingCycles().isEmpty()) {
            filters.add(Criteria.where("billingCycle").in(request.billingCycles()));
        }

        //! step 4 - the plan, already resolved from a code to the version ids it names. Null
        //! means no planCode was sent; EMPTY means one was sent and matched no plan, which has
        //! to match nothing rather than everything — an `in []` is what says that.
        if (planDefinitionDocsIds != null) {
            filters.add(Criteria.where("planDefinitionDocsId").in(planDefinitionDocsIds));
        }

        //! step 5 - the version, stored on the subscription so it filters on its own
        if (request.planVersion() != null) {
            filters.add(Criteria.where("planVersion").is(request.planVersion()));
        }

        //! step 6 - the two standing flags
        if (request.autoRenew() != null) {
            filters.add(Criteria.where("autoRenew").is(request.autoRenew()));
        }
        if (request.current() != null) {
            filters.add(Criteria.where("current").is(request.current()));
        }

        //! step 7 - the period windows. Each end is INCLUSIVE (gte / lte), because a caller
        //! filtering "from 1 April" means a period that starts on 1 April is in, and an
        //! exclusive bound would silently drop the row they were looking for.
        //!
        //! The two ends of one window go on the SAME criteria object rather than as two
        //! separate `where` clauses: two clauses on one field is a document with two keys of the
        //! same name, and Mongo keeps only the last — so a from/to pair would filter on `to`
        //! alone and quietly return too much.
        if (request.startDateFrom() != null || request.startDateTo() != null) {
            Criteria start = Criteria.where("currentPeriodStart");
            if (request.startDateFrom() != null) {
                start = start.gte(request.startDateFrom());
            }
            if (request.startDateTo() != null) {
                start = start.lte(request.startDateTo());
            }
            filters.add(start);
        }

        if (request.endDateFrom() != null || request.endDateTo() != null) {
            Criteria end = Criteria.where("currentPeriodEnd");
            if (request.endDateFrom() != null) {
                end = end.gte(request.endDateFrom());
            }
            if (request.endDateTo() != null) {
                end = end.lte(request.endDateTo());
            }
            filters.add(end);
        }

        //! step 8 - AND them together. The school id alone is the no-filter case.
        return new Criteria().andOperator(filters);
    }
}
