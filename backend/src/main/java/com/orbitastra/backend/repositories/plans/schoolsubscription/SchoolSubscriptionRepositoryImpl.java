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

import com.orbitastra.backend.dto.plans.subscription.request.PlatformSubscriptionSearchRequest;
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

    @Override
    public Page<SchoolSubscription> searchAcrossSchools(PlatformSubscriptionSearchRequest request,
            Collection<String> planDefinitionDocsIds, Pageable pageable) {

        //! step 1 - build the filter. NO tenant clause: this is the cross-school view, and the
        //! absence is the feature rather than an omission — see the interface.
        Criteria criteria = buildPlatformCriteria(request, planDefinitionDocsIds);

        //! step 2 - the count query, carrying the filter and nothing else
        Query countQuery = new Query(criteria);

        //! step 3 - the page query, the same filter plus the paging and sorting
        Query pageQuery = new Query(criteria).with(pageable);

        //! step 4 - run the count, for totalElements
        // TODO: reading subscriptions across schools (how many match)
        long total = mongo.count(countQuery, SchoolSubscription.class);

        //! step 5 - run the page. Only these rows are read, however many schools there are.
        // TODO: reading subscriptions across schools (one page of them)
        List<SchoolSubscription> rows = mongo.find(pageQuery, SchoolSubscription.class);

        //! step 6 - hand back the rows with the total beside them
        return new PageImpl<>(rows, pageable, total);
    }

    /**
     * #30's filters, combined with AND. An absent one adds nothing, so no filters means every
     * subscription on the platform.
     *
     * <p><b>Deliberately not shared with {@link #buildCriteria} above.</b> They look alike and
     * they are not the same: that one starts by pinning the tenant and this one must not. Folding
     * them together behind a nullable school id would make the tenant boundary an argument that
     * can be forgotten, on the one collection where forgetting it means answering one school's
     * request with another's records. Two readable methods beat one clever one here.
     *
     * <p>An empty {@code $and} is not possible — Mongo rejects it — so this returns a bare
     * {@link Criteria} when nothing was filtered, which matches everything.
     */
    private Criteria buildPlatformCriteria(PlatformSubscriptionSearchRequest request,
            Collection<String> planDefinitionDocsIds) {

        //! step 1 - nothing is pinned first here. Start empty.
        List<Criteria> filters = new ArrayList<>();

        //! step 2 - status, the headline filter, which ORs within itself
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
        //! filtering "ending before 31 March" means a period ending on 31 March is in, and an
        //! exclusive bound would silently drop the row they were looking for. Both ends of one
        //! window go on the same criteria object, which builds one clause; see the note on
        //! buildCriteria about what that is and is not for.
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

        //! step 8 - AND them together, unless nothing was sent at all. `andOperator` of an empty
        //! list produces `{$and: []}`, which Mongo refuses outright, so a bare list has to be a
        //! bare Criteria — the one shape difference from the school-scoped query, which always
        //! has at least the tenant in its list.
        return filters.isEmpty() ? new Criteria() : new Criteria().andOperator(filters);
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
        //! Both ends go on the SAME criteria object, which builds one
        //! `{currentPeriodStart: {$gte: .., $lte: ..}}` clause.
        //!
        //! CORRECTION (2026-09-09, while building #29): this used to say two separate `where`
        //! clauses would lose one because a document cannot repeat a key. That is not true of
        //! this construction — `andOperator` gives each criteria its own element of the `$and`
        //! ARRAY, so both ends apply either way, and splitting them was mutation-tested here and
        //! changed no result. One object is a clarity choice, and the form that stays correct if
        //! these are ever merged with `.and()`, which is where a repeated key really collides.
        //! The place the duplicate key genuinely bit was the SORT document — see
        //! PageResponse.sortOf, where `?sort=planCode,desc` silently sorted ascending.
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
