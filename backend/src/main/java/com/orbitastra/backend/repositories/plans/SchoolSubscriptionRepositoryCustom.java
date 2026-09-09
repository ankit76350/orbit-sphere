package com.orbitastra.backend.repositories.plans;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
