package com.orbitastra.backend.repositories.plans.schoolsubscription;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.plans.subscription.request.PlatformSubscriptionSearchRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionSearchRequest;
import com.orbitastra.backend.models.plans.SchoolSubscription;

/**
 * The part of {@link SchoolSubscriptionRepository} that cannot be a derived query method.
 *
 * <p>#28's filters are each optional, so no method name could express it: a name per combination,
 * and none of them matching a request that sends no filters at all. The same reasoning, and the
 * same shape, as {@link PlanDefinitionRepositoryCustom}.
 *
 * <p>Spring Data finds the implementation by name — {@code SchoolSubscriptionRepositoryImpl}.
 * Renaming that class breaks the wiring at startup, silently, so do not.
 */
public interface SchoolSubscriptionRepositoryCustom {

    /**
     * One school's subscriptions, filtered, sorted and paged in the database.
     *
     * <p><b>{@code schoolId} is separate from the request on purpose.</b> It is the tenant
     * boundary rather than a filter: it is always applied, it can never be omitted, and it is
     * not something a caller sends. Putting it in the filter record would make it look optional.
     *
     * @param planDefinitionDocsIds the plan versions the {@code planCode} filter resolved to, or
     *                              {@code null} when no {@code planCode} was sent. An <b>empty</b>
     *                              collection means the code matched no plan, which is a filter
     *                              that matches nothing rather than no filter at all.
     */
    Page<SchoolSubscription> search(String schoolId, SubscriptionSearchRequest request,
            Collection<String> planDefinitionDocsIds, Pageable pageable);

    /**
     * Every school's subscriptions, filtered, sorted and paged in the database. #30.
     *
     * <p><b>There is no school id, and that is the whole point.</b> This is the operator's
     * cross-school view, so the tenant filter that every other query on this collection applies
     * unconditionally is simply absent here. That makes it the one method on this repository that
     * can return two schools' rows in one page, so it is deliberately separate from
     * {@link #search} above rather than that method taking a nullable school — a nullable tenant
     * is one {@code if} away from leaking one school's rows into another's request.
     *
     * @param planDefinitionDocsIds the plan versions the {@code planCode} filter resolved to, or
     *                              {@code null} when no {@code planCode} was sent. An <b>empty</b>
     *                              collection means the code matched no plan, which is a filter
     *                              that matches nothing rather than no filter at all.
     */
    Page<SchoolSubscription> searchAcrossSchools(PlatformSubscriptionSearchRequest request,
            Collection<String> planDefinitionDocsIds, Pageable pageable);
}
