package com.orbitastra.backend.controllers.plans;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.plans.subscription.SubscriptionCreateRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionDetailResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionPlanChangeRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionRenewRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionResumeRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionSuspendRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionUpdateRequest;
import com.orbitastra.backend.services.plans.PlatformSubscriptionService;

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
 * school surface; creating one, moving its dates and raising its limits are all here, so there
 * is no request a school can send that does them.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/platform/schools/{schoolId}")
public class PlatformSubscriptionController {

    private final PlatformSubscriptionService subscriptionService;

        /**
         * Endpoint #13 — creates a school's first subscription.
         *
         * <p>Plan is selected by code and version. Price, currency, cycle, and period end
         * come from the plan.
         *
         * <p>Creates the subscription, history, and subscription number in one transaction.
         */
    @PostMapping("/subscriptions")
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
     * Endpoint #14 — edits the terms of one subscription.
     *
     * <p>Use {@code current} as the subscription number for the one the school is on now.
     *
     * <p><b>When it runs, what state it is in, how much of the product it may use.</b> The status,
     * the billing cycle, both period dates, auto-renewal, the two capacity overrides and the
     * cancellation — three endpoints' worth of single-column edits (extend-trial, #23 and #24) in
     * one request. Pushing a trial's end date out is {@code currentPeriodEnd} here.
     *
     * <p><b>Nothing about the money.</b> The price and currency stay on #25, the billing customer
     * on #26 and the plan on #16, because changing what a school pays has invoice consequences
     * that should not ride along with a change of dates.
     *
     * <p>Every field is optional and absent means unchanged. The request documents what each
     * field's absence means, and which two are nested so that "remove this" can be told apart
     * from "leave it alone".
     *
     * <p>Answers the whole subscription back, the same shape as #27, so a caller does not have to
     * read it again to see what it now says. Nothing to change is {@code 400
     * NO_CHANGES_REQUESTED}; a request that only restates what is already stored is a 200 that
     * says nothing changed.
     */
    @PatchMapping("/subscriptions/{subscriptionNo}")
    public ResponseEntity<SubscriptionDetailResponse> update(
            @PathVariable String schoolId,
            @PathVariable String subscriptionNo,
            @Valid @RequestBody SubscriptionUpdateRequest request) {

        return ResponseEntity.ok(
                subscriptionService.updateSubscription(schoolId, subscriptionNo, request));
    }

    /**
     * Endpoint #16 — moves a school onto a different plan, or a newer version of its own.
     *
     * <p>Use {@code current} as the subscription number for the one the school is on now.
     *
     * <p><b>What #14 cannot do.</b> #14 edits the terms of the plan a school is already on; this
     * changes which plan that is, and with it the entitlements, the price and the billing cycle.
     *
     * <p><b>It takes effect immediately.</b> There is no scheduling: a subscription holds one
     * plan, so a change set for the next period would have nowhere to live. The plan moves now
     * and the billing period restarts with it.
     *
     * <p><b>No money moves, and nothing is asked about it.</b> Nothing raises invoices yet, so
     * this endpoint charges, credits and refunds nothing for the period the school had already
     * paid for — and the response says so plainly rather than leaving it to be assumed.
     *
     * <p>Answers the whole subscription back, the same shape as #27.
     */
    @PostMapping("/subscriptions/{subscriptionNo}/change-plan")
    public ResponseEntity<SubscriptionDetailResponse> changePlan(
            @PathVariable String schoolId,
            @PathVariable String subscriptionNo,
            @Valid @RequestBody SubscriptionPlanChangeRequest request) {

        return ResponseEntity.ok(
                subscriptionService.changePlan(schoolId, subscriptionNo, request));
    }

    
    /**
     * Endpoint #17 — renews the subscription for the next billing period.
    */
    @PostMapping("/subscriptions/{subscriptionNo}/renew")
    public ResponseEntity<SubscriptionDetailResponse> renew(
            @PathVariable String schoolId,
            @PathVariable String subscriptionNo,
            // Not required: the ordinary renewal sends nothing, and a POST with no body at all
            // has to keep working. It arrives as null, which the service reads as "derive the
            // period from the cycle" — the only cycle that cannot is CUSTOM, and it says so.
            @Valid @RequestBody(required = false) SubscriptionRenewRequest request) {

        return ResponseEntity.ok(
                subscriptionService.renewSubscription(schoolId, subscriptionNo, request));
    }

    /**
     * Endpoint #19 — cuts a school off for non-payment.
     *
     * <p>Use {@code current} as the subscription number for the one the school is on now.
     *
     * <p><b>Not the same as #14 writing a status.</b> Cutting a school off stops its staff
     * working, so it is one transition with its own rules rather than a field on an edit — and it
     * carries the school's own access with it, which #14 does not.
     *
     * <p><b>Two documents move.</b> The subscription goes {@code SUSPENDED}, turning every
     * feature off through #34; the school goes {@code SUSPENDED} too, which is what blocks the
     * tenant. {@code ACTIVE} and {@code PAST_DUE} only — a trial has no unpaid bill behind it —
     * and a {@code reason} is required.
     *
     * <p><b>It does not kill live sessions or stop scheduled jobs</b>, because neither exists
     * yet. The response says so rather than leaving it assumed.
     */
    @PostMapping("/subscriptions/{subscriptionNo}/suspend")
    public ResponseEntity<SubscriptionDetailResponse> suspend(
            @PathVariable String schoolId,
            @PathVariable String subscriptionNo,
            @Valid @RequestBody SubscriptionSuspendRequest request) {

        return ResponseEntity.ok(
                subscriptionService.suspendSubscription(schoolId, subscriptionNo, request));
    }

    /**
     * Endpoint #20 — switches a school back on after it pays.
     *
     * <p>The exact reverse of #19: the subscription returns to {@code ACTIVE} and the school with
     * it. Only a {@code SUSPENDED} subscription can be resumed, and a {@code reason} is required.
     *
     * <p><b>The period is not extended.</b> A school suspended for three weeks comes back to the
     * same {@code currentPeriodEnd}, having paid for time it could not use — crediting that is a
     * money decision nothing here can make, so the response says so instead of quietly moving
     * the date. #14 is where a date moves if a credit was agreed.
     */
    @PostMapping("/subscriptions/{subscriptionNo}/resume")
    public ResponseEntity<SubscriptionDetailResponse> resume(
            @PathVariable String schoolId,
            @PathVariable String subscriptionNo,
            @Valid @RequestBody SubscriptionResumeRequest request) {

        return ResponseEntity.ok(
                subscriptionService.resumeSubscription(schoolId, subscriptionNo, request));
    }

    /**
     * Endpoint #27 — what this school is on right now.
     *
     * <p>The whole of it: the plan and its features, the price they actually pay against the
     * plan's list price, the status, and when the period ends.
     *
     * <p><b>Singular, because a school has one.</b> {@code /subscriptions} is the collection you
     * post to; {@code /subscription} is the one they are on. A unique partial index makes sure
     * there is only ever one, so there is nothing to page through.
     *
     * <p>A school with none gets {@code 404 SUBSCRIPTION_NOT_FOUND}, and a school that does not
     * exist gets {@code 404 SCHOOL_NOT_FOUND} — different problems, different answers.
     */
    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionDetailResponse> getSubscription(
            @PathVariable String schoolId) {

        return ResponseEntity.ok(subscriptionService.getSubscription(schoolId));
    }
}
