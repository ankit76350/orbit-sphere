package com.orbitastra.backend.controllers.plans;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.plans.subscription.SubscriptionActivateRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionCreateRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionResponse;
import com.orbitastra.backend.services.plans.SubscriptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * What one school bought. Endpoints #13 onwards of the plan in this package's README.
 *
 * <p><b>Platform surface, and the school is named in the URL</b> — the operator is outside the
 * tenant, so there is no session to read it from. That is the opposite of the school surface,
 * where a school never names itself; both rules exist for the same reason, which is that a
 * caller should only ever be able to reach the school they are entitled to.
 *
 * <p>A separate controller from [`PlanController`] because these are separate resources with
 * separate lifecycles: a plan version is platform configuration shared by everybody, a
 * subscription belongs to one school. They sit in the same module because the money only makes
 * sense with both.
 *
 * <p><b>A school may not reach any of this.</b> Looking at its own subscription is #33 on the
 * school surface; creating one, changing its price, extending its trial and raising its limits
 * are all here, so there is no request a school can send that does them.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/platform/schools/{schoolId}/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

        /**
         * Endpoint #13 — creates a school's first subscription.
         *
         * <p>Plan is selected by code and version. Price, currency, cycle, and period end
         * come from the plan.
         *
         * <p>Creates the subscription, history, and subscription number in one transaction.
         */
    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @PathVariable String schoolId,
            @Valid @RequestBody SubscriptionCreateRequest request) {

        SubscriptionResponse response = subscriptionService.createSubscription(schoolId, request);

        return ResponseEntity
                .created(URI.create("/platform/schools/" + schoolId + "/subscriptions/"
                        + response.subscriptionNo()))
                .body(response);
    }
        /**
         * Endpoint #14 — converts a trial into a paid subscription.
         *
         * <p>Use {@code current} as the subscription number. The plan, price, and limits remain unchanged;
         * a new paid billing period starts.
         *
         * <p>The body is optional. If omitted, the paid period starts now for one billing cycle.
         * Dates or a reason can be provided for non-standard activation.
         *
         * <p>Only a TRIAL can be activated; otherwise returns {@code 409 SUBSCRIPTION_NOT_TRIAL}.
        */
    @PostMapping("/{subscriptionNo}/activate")
    public ResponseEntity<SubscriptionResponse> activate(
            @PathVariable String schoolId,
            @PathVariable String subscriptionNo,
            @Valid @RequestBody(required = false) SubscriptionActivateRequest request) {

        return ResponseEntity.ok(
                subscriptionService.activateSubscription(schoolId, subscriptionNo, request));
    }
}
