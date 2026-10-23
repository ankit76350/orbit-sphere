package com.orbitastra.backend.repositories.plans.subscriptionhistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionHistorySearchRequest;
import com.orbitastra.backend.models.plans.SubscriptionHistory;

/**
 * The part of {@link SubscriptionHistoryRepository} that cannot be a derived query method.
 *
 * <p>#29's filters are each optional, so no method name could express it: a name per combination,
 * and none of them matching a request that sends no filters at all. The same reasoning, and the
 * same shape, as {@code SchoolSubscriptionRepositoryCustom}.
 *
 * <p><b>Reads only, and it must stay that way.</b> The collection is append-only — see the note
 * on {@link SubscriptionHistoryRepository} — so nothing here returns anything but rows.
 *
 * <p>Spring Data finds the implementation by name <b>and package</b>:
 * {@code SubscriptionHistoryRepositoryImpl}, beside this interface. Moving or renaming that class
 * still compiles and still starts, and fails only when somebody calls the method. So do not.
 */
public interface SubscriptionHistoryRepositoryCustom {

    /**
     * One subscription's trail, filtered, sorted and paged in the database.
     *
     * <p><b>{@code schoolId} and {@code schoolSubscriptionDocsId} are separate from the request on
     * purpose.</b> They are the boundary rather than filters: both are always applied, neither can
     * be omitted, and neither is something a caller sends as a parameter. Putting them in the
     * filter record would make them look optional.
     *
     * @param schoolId                 the tenant. Applied even though the subscription id alone is
     *                                 globally unique, so a bug in resolving the subscription
     *                                 cannot become one school reading another's audit trail.
     * @param schoolSubscriptionDocsId the subscription whose trail this is
     */
    Page<SubscriptionHistory> search(String schoolId, String schoolSubscriptionDocsId,
            SubscriptionHistorySearchRequest request, Pageable pageable);
}
