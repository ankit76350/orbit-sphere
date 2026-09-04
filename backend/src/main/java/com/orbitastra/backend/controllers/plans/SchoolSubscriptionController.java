package com.orbitastra.backend.controllers.plans;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.dto.plans.subscription.EntitlementsResponse;
import com.orbitastra.backend.dto.plans.subscription.MySubscriptionResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.plans.SchoolSubscriptionService;
import com.orbitastra.backend.services.plans.PlatformSubscriptionService;

import lombok.RequiredArgsConstructor;

/**
 * What a school can see about its own subscription. Endpoints #33 and #34.
 *
 * <p><b>The path is {@code /schools/current}, never {@code /schools/{id}}.</b> The tenant comes
 * from {@link CurrentSchoolResolver} — today a header, tomorrow the session — and never from the
 * URL. With no id in the path a caller cannot name a school it does not belong to, because it
 * never names one at all. Reading another school's bill would otherwise be a matter of editing a
 * URL.
 *
 * <p><b>Reads only.</b> This is the whole of the school surface for subscriptions: a school may
 * see what it is on and what it may use. Creating one, changing its price, extending a trial and
 * raising a limit are all on the platform surface in
 * {@link PlatformSubscriptionController}, so there is no request a school can send that does any of
 * them.
 *
 * <p><b>There is no authentication yet.</b> The tenant header is a stand-in and any caller can
 * set it to any school's subdomain, which means anybody can read any school's billing. Fine on a
 * developer machine, unacceptable anywhere else — and it matters more here than on most
 * endpoints, because this is somebody's commercial terms.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/schools/current/subscription")
public class SchoolSubscriptionController {

    private final CurrentSchoolResolver currentSchool;
    private final PlatformSubscriptionService subscriptionService;
    private final SchoolSubscriptionService entitlementService;

    /**
     * Endpoint #33 — the school's billing screen.
     *
     * <p>What plan it is on, what it costs, when the period ends, and whether it renews.
     * Deliberately less than the platform's own read of the same subscription (#27).
     *
     * <p>{@code require()} rather than {@code requireUsable()}: a suspended or closed school
     * should still be able to read why. Refusing to show the billing screen to exactly the
     * school that needs to look at it would be the wrong way round.
     */
    @GetMapping
    public ResponseEntity<MySubscriptionResponse> getMySubscription() {
        School school = currentSchool.require();
        return ResponseEntity.ok(subscriptionService.getMySubscription(school));
    }

    /**
     * Endpoint #34 — what this school is allowed to use.
     *
     * <p><b>The one the rest of the product asks.</b> No module may read the plan's features and
     * decide for itself whether a school can use something: two places working that out disagree,
     * and they disagree in the direction of letting a school use what it has not paid for.
     *
     * <p>A gate reads {@code allowed} on the feature it cares about and nothing else — the
     * subscription's own state is already folded into that field, so forgetting the top-level
     * {@code active} flag cannot produce a wrong answer.
     *
     * <p>In-process callers should use {@link SchoolSubscriptionService} directly rather than making an
     * HTTP request to themselves. This endpoint is the same method with a URL in front of it.
     */
    @GetMapping("/entitlements")
    public ResponseEntity<EntitlementsResponse> getEntitlements() {
        School school = currentSchool.require();
        return ResponseEntity.ok(entitlementService.entitlementsFor(school));
    }
}
