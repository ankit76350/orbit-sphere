package com.orbitastra.backend.services.plans;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionActivateRequest;
import com.orbitastra.backend.dto.plans.subscription.MySubscriptionResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionDetailResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionCreateRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionUpdateRequest;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.institution.enums.NumberSequenceType;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.SubscriptionHistory;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;
import com.orbitastra.backend.models.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.plans.PlanDefinitionRepository;
import com.orbitastra.backend.repositories.plans.SchoolSubscriptionRepository;
import com.orbitastra.backend.repositories.plans.SubscriptionHistoryRepository;
import com.orbitastra.backend.services.core.SchoolPlatformService;
import com.orbitastra.backend.services.institution.NumberSequenceService;
import com.orbitastra.backend.services.plans.helper.PlanValidator;

import lombok.RequiredArgsConstructor;

/**
 * What a school actually bought. Endpoints #13 onwards.
 *
 * <p>Where the plan catalogue says what is for sale, this says who is paying for what. The
 * distinction runs through the whole module: a plan version is platform configuration shared by
 * everybody, a subscription belongs to one school.
 *
 * <p><b>Platform surface only.</b> A school may look at its own subscription (#33) and pay its
 * own bills, but it may not create one, change its own price, or raise its own limits — so those
 * endpoints do not exist on the school surface at all.
 *
 * <h2>Every status change writes a history row, in the same transaction</h2>
 *
 * <p>{@code models/plans/README.md} asks for it and the reason is worth restating: months later,
 * "why is this school suspended" has to have an answer. A history row written in a second
 * transaction is a row that can go missing exactly when the thing it explains went wrong.
 */
@Service
@RequiredArgsConstructor
public class PlatformSubscriptionService {

    /** Written on every history row this service creates. */
    private static final String SOURCE_ADMIN_PORTAL = "ADMIN_PORTAL";

    /**
     * What to put in the URL instead of a subscription number, to mean "the one this school is
     * on now".
     *
     * <p><b>This exists because a subscription number cannot go in a URL.</b> The house format
     * is {@code SUB/2026/09/000001} — it has slashes in it, and a slash ends a path segment, so
     * {@code .../subscriptions/SUB/2026/09/000001/activate} is not the address of anything.
     * Writing them as {@code %2F} does not help either: Tomcat rejects an encoded slash in a
     * path with a 400 before Spring ever sees it.
     *
     * <p>So the word {@code current} is used instead, and it is not a workaround so much as the
     * honest name for what is being asked. A school has exactly one current subscription — a
     * unique index makes sure of it — so there was never a choice to make here.
     */
    private static final String CURRENT_SUBSCRIPTION = "current";

    private final SchoolRepository schools;
    private final PlanDefinitionRepository planDefinition;
    private final SchoolSubscriptionRepository schoolSubscription;
    private final SubscriptionHistoryRepository history;
    private final NumberSequenceService numberSequences;
    private final PlanValidator planValidator;

    /**
     * Only for the two setup gates and nothing else.
     *
     * <p>A subscription takes a school live, and going live has conditions that belong to core: a
     * SCHOOL_ADMIN role and the number sequences. Asking core rather than re-checking them here
     * means one implementation — two copies of "is this school ready" is how the two come to
     * disagree, and the wrong copy is the one that lets a broken school go live.
     */
    private final SchoolPlatformService schoolPlatform;

    //! endpoint 13 — a school's first subscription -------------------------------------

    /**
     * #13 — puts a school on a plan. What makes a school a paying customer.
     *
     * <p><b>This is the piece the core module has been complaining about.</b>
     * {@code activateSchool} was written to require an active subscription, found that nothing
     * could create one, and had to settle for a soft check that lets a school go live with no
     * subscription at all — announcing the gap in every response rather than pretending. This
     * closes it.
     *
     * <p>Three documents, one transaction: the subscription, its first history row, and the
     * number sequence it took its number from. A subscription without its history row is a
     * customer nobody can explain, and a number handed out without a subscription to attach it
     * to is a gap in the numbering that looks like a deleted record for ever.
     *
     * <p><b>Most of the request is optional.</b> Price, currency, cycle and the period end all
     * come from the plan unless the caller says otherwise — the ordinary case is "put them on
     * Premium v1", and the fields exist for the deal that is not ordinary.
     */
    @Transactional
    public SubscriptionResponse createSubscription(String schoolId,
            SubscriptionCreateRequest request) {

        //! step 1 - the school has to exist and be a school we can still sell to
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        if (school.getStatus() == SchoolStatus.DELETED
                || school.getStatus() == SchoolStatus.DELETION_PENDING
                || school.getStatus() == SchoolStatus.CLOSED) {
            throw ApiException.conflict("SCHOOL_NOT_SUBSCRIBABLE",
                    "'" + school.getSchoolName() + "' is " + school.getStatus() + " and cannot "
                            + "be given a subscription.");
        }

        //! step 2 - one current subscription per school. The unique partial index enforces it,
        //! but a duplicate-key error tells the caller nothing about what to do instead.
        // TODO: read school subscription
        schoolSubscription.findBySchoolIdAndCurrentIsTrue(schoolId).ifPresent(existing -> {
            throw ApiException.conflict("SUBSCRIPTION_ALREADY_EXISTS",
                    "'" + school.getSchoolName() + "' is already on " + existing.getSubscriptionNo()
                            + ". Change the plan on that subscription rather than creating a "
                            + "second one.");
        });

        //! step 3 - the plan has to be one we can actually sell today
        PlanDefinition plan = loadSellablePlan(request.planCode(), request.planVersion());

        //! step 4 - work out the terms: the plan's, unless the caller overrode them
        Instant periodStart = request.currentPeriodStart() == null
                ? Instant.now()
                : request.currentPeriodStart();
        Instant periodEnd = resolvePeriodEnd(request.currentPeriodEnd(), periodStart,
                plan.getBillingCycle(), school.getDefaultTimeZone());

        BigDecimal contractedPrice = request.contractedPrice() == null
                ? plan.getListPrice()
                : planValidator.validatePrice("contractedPrice", request.contractedPrice());

        validateOverride("maxStudentsOverride", request.maxStudentsOverride());
        validateOverride("maxUsersOverride", request.maxUsersOverride());

        boolean trial = Boolean.TRUE.equals(request.trial());
        SubscriptionStatus status = trial ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE;

        //! step 5 - take a subscription number. Atomic, so two requests can never be handed the
        //! same one. See NumberSequenceService.
        String subscriptionNo = numberSequences.next(schoolId, NumberSequenceType.SUBSCRIPTION,
                "SUB/{YYYY}/{MM}/");

        //! step 6 - build it
        SchoolSubscription subscription = SchoolSubscription.builder()
                .schoolId(schoolId)
                .subscriptionNo(subscriptionNo)
                .planDefinitionDocsId(plan.getId())
                .planVersion(plan.getPlanVersion())
                .status(status)
                .billingCycle(plan.getBillingCycle())
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .autoRenew(request.autoRenew() == null ? Boolean.TRUE : request.autoRenew())
                .contractedPrice(contractedPrice)
                // The currency comes from the plan, never the caller: a subscription priced in a
                // different currency from the plan it points at is a mistake nobody would catch
                // until an invoice went out in the wrong money.
                .currencyCode(plan.getCurrencyCode())
                .maxStudentsOverride(request.maxStudentsOverride())
                .maxUsersOverride(request.maxUsersOverride())
                .billingCustomerReference(request.billingCustomerReference())
                .current(true)
                .build();

        // TODO: insert school subscription
        SchoolSubscription savedSubscription = schoolSubscription.save(subscription);

        //! step 7 - its first history row, in this same transaction
        SubscriptionHistory subscriptionHistory = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(savedSubscription.getId())
                .eventType(trial ? SubscriptionEventType.TRIAL_STARTED : SubscriptionEventType.CREATED)
                .previousStatus(null)
                .newStatus(status)
                .newPlanDefinitionDocsId(plan.getId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason(request.reason())
                .performedByDocsId(null)
                .effectiveAt(periodStart)
                .build();

        // TODO: insert history
        history.save(subscriptionHistory);

        //! step 8 - a school that was only waiting on a subscription can go live now
        String activation = activateIfReady(school);

        return SubscriptionResponse.fromSubscription(savedSubscription, plan,
                nextStepFor(savedSubscription, trial, activation));
    }

    //! endpoint 14 — a trial becomes a paying subscription ----------------------------

        /**
         ** #14 — converts a trial into a paid subscription.
         *
         * <p>Plan, price, and limits remain unchanged; the trial ends and a fresh paid period starts.
         *
         * <p>Only TRIAL subscriptions can be activated. ACTIVE, CANCELLED, or EXPIRED subscriptions
         * are rejected with an appropriate response.
         *
         * <p>The plan is not revalidated, so a retired plan can still be activated for an existing trial.
         *
         * <p>Subscription and history are updated in one transaction.
        */
    @Transactional
    public SubscriptionResponse activateSubscription(String schoolId, String subscriptionNo,
            SubscriptionActivateRequest request) {

        SubscriptionActivateRequest asked = request == null
                ? SubscriptionActivateRequest.empty()
                : request;

        //! step 1 - the school has to exist
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - find the subscription. "current" means the one the school is on now, which
        //! is how it is normally addressed, because a real subscription number has slashes in
        //! it and will not fit in a URL. The school id is in the lookup either way, so one
        //! school cannot reach another school's subscription by guessing its number.
        SchoolSubscription subscription = findSubscription(school, schoolId, subscriptionNo);

        //! step 3 - it has to be a trial. Anything else is refused, with advice that fits.
        requireTrial(subscription);

        //! step 4 - work out the paid period. It starts now unless the caller said otherwise,
        //! and runs for one of whatever cycle this subscription was sold on.
        Instant periodStart = asked.currentPeriodStart() == null
                ? Instant.now()
                : asked.currentPeriodStart();
        Instant periodEnd = resolvePeriodEnd(asked.currentPeriodEnd(), periodStart,
                subscription.getBillingCycle(), school.getDefaultTimeZone());

        //! step 5 - save the change
        SubscriptionStatus previousStatus = subscription.getStatus();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        // TODO: update school subscription
        SchoolSubscription saved = schoolSubscription.save(subscription);

        //! step 6 - write down that it happened, in this same transaction
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(SubscriptionEventType.ACTIVATED)
                .previousStatus(previousStatus)
                .newStatus(SubscriptionStatus.ACTIVE)
                .newPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason(asked.reason())
                .performedByDocsId(null)
                .effectiveAt(periodStart)
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 7 - the plan is only read so the response can name it and show the limits
        // TODO: read plan
        PlanDefinition plan = planDefinition.findById(saved.getPlanDefinitionDocsId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "The plan this subscription points at no longer exists."));

        return SubscriptionResponse.fromSubscription(saved, plan, activatedNextStep(school, saved));
    }

    //! endpoint 15 — editing what a school is contracted to ---------------------------

    /**
     * #15 — edits any of the terms of one subscription.
     *
     * <p><b>This is where extend-trial went, along with #23 and #24.</b> Three endpoints for
     * three columns of one document meant three sets of rules, and a correction that touched two
     * of them was two requests, two writes and two history rows for one decision. One PATCH, one
     * transaction, one history row saying what moved.
     *
     * <p><b>Nothing about the money is here.</b> The price and currency are #25, the billing
     * customer is #26, the plan is #16 — see the request for why. What is here is when the
     * subscription runs, what state it is in, and how much of the product it may use.
     *
     * <p><b>It applies no transition rules, on purpose.</b> The lifecycle endpoints each know one
     * transition and what it implies — renewing raises an invoice, cancelling decides what
     * happens to money already paid. This writes what it is told, which is exactly what is needed
     * when a subscription is already wrong and no ordinary transition describes the fix. It is
     * not how a subscription should ordinarily be renewed or cancelled.
     *
     * <p><b>One thing is still not negotiable.</b> A billing period cannot be made to run
     * backwards, because there is no reading of that which is not a mistake.
     *
     * <p><b>Nothing is written when nothing changed.</b> A request that sets every field to what
     * it already holds answers 200 and says so, with no history row: an audit trail whose rows
     * record that nothing happened is one nobody can read. That includes the reason — an
     * explanation for an edit that did not happen is not worth storing.
     *
     * <p><b>{@code reason} lands in two places.</b> On the subscription as
     * {@code reasonForChanges}, so a screen can say why it looks the way it does without a second
     * query, and on the history row next to the fields that moved. The document keeps the latest;
     * history keeps all of them.
     */
    @Transactional
    public SubscriptionDetailResponse updateSubscription(String schoolId, String subscriptionNo,
            SubscriptionUpdateRequest request) {

        //! step 1 - a request that asks for nothing is a mistake, not a no-op. Answering 200 to
        //! it would tell a caller who sent the wrong field name that their edit worked.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NO_CHANGES_REQUESTED",
                    "Nothing to change. Send at least one of the fields this endpoint edits.");
        }

        //! step 2 - the school has to exist, and still be one whose records mean anything
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        // CLOSED is deliberately allowed: a school that has left still has a subscription whose
        // record can need correcting, and refusing would leave the wrong figure in place for
        // ever. A deleted one is different — there is nothing left to be right about.
        if (school.getStatus() == SchoolStatus.DELETED
                || school.getStatus() == SchoolStatus.DELETION_PENDING) {
            throw ApiException.conflict("SUBSCRIPTION_NOT_EDITABLE",
                    "'" + school.getSchoolName() + "' is " + school.getStatus()
                            + ", so its subscription is no longer editable.");
        }

        //! step 3 - the subscription named in the URL, or the one they are on now
        SchoolSubscription subscription = findSubscription(school, schoolId, subscriptionNo);

        //! step 4 - apply the edit, keeping a list of what actually moved. The list is what the
        //! history row and the response are built from, so "changed" means "different from what
        //! was stored", not "was mentioned in the request".
        SubscriptionStatus previousStatus = subscription.getStatus();
        List<String> changed = applyEdit(subscription, request);

        if (changed.isEmpty()) {
            // TODO: read plan
            PlanDefinition unchangedPlan = loadPlanBehind(subscription);
            return SubscriptionDetailResponse.fromSubscription(subscription, unchangedPlan,
                    "Nothing changed: every field sent already held that value. No history row "
                            + "was written.");
        }

        //! step 5 - the reason goes onto the document as well as the history row. Written only
        //! now, because an edit that changed nothing has nothing to explain — and overwritten
        //! even when no reason was given, since a reason left over from an earlier edit would
        //! explain the wrong change.
        subscription.setReasonForChanges(
                request.reason() == null || request.reason().isBlank()
                        ? null
                        : request.reason().trim());

        //! step 6 - the period has to still make sense after the edit, whichever end moved
        if (!subscription.getCurrentPeriodEnd().isAfter(subscription.getCurrentPeriodStart())) {
            throw ApiException.badRequest("INVALID_BILLING_PERIOD",
                    "currentPeriodEnd (" + subscription.getCurrentPeriodEnd() + ") must be after "
                            + "currentPeriodStart (" + subscription.getCurrentPeriodStart()
                            + "). Editing one end of a period is checked against the other.");
        }

        // TODO: update school subscription
        SchoolSubscription saved = schoolSubscription.save(subscription);

        //! step 7 - one history row for the whole edit, in this same transaction
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(eventTypeFor(previousStatus, saved.getStatus()))
                .previousStatus(previousStatus)
                .newStatus(saved.getStatus())
                // The plan cannot move here, so there is no previous plan to record — both ids
                // are the one it is still on. #16 is what writes a plan change.
                .newPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason(auditReason(changed, request.reason()))
                .performedByDocsId(null)
                .effectiveAt(Instant.now())
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 8 - the plan is read only so the response can carry its features and limits
        // TODO: read plan
        PlanDefinition plan = loadPlanBehind(saved);

        return SubscriptionDetailResponse.fromSubscription(saved, plan,
                editedNote(saved, plan, changed));
    }

    //! endpoint 27 — what one school is on right now ----------------------------------

    /**
     * #27 — the whole of one school's current subscription.
     *
     * <p>The platform read. Everything about what a school is on: the plan and its features, the
     * price they actually pay against the plan's list price, the status, and when the period
     * ends.
     *
     * <p><b>Two reads on the way through, and a third only when something is wrong.</b> The happy
     * path is the subscription and then the plan it points at. If there is no subscription, the
     * school is looked up before answering, because "no such school" and "that school has no
     * subscription" are different problems and a single 404 for both sends people looking in the
     * wrong place. That read costs nothing on the path that succeeds.
     *
     * <p><b>It reports a lapsed period rather than hiding it.</b> Nothing renews a subscription
     * or marks one expired yet — #21 and #26 are not built — so a period can run out while the
     * status still says the school is paying. The response says so in {@code periodEnded} and in
     * {@code note}, because a screen trusting {@code status} alone would show a school as live
     * months after its period ended.
     */
    public SubscriptionDetailResponse getSubscription(String schoolId) {

        //! step 1 - the school's one current subscription
        // TODO: read subscription
        SchoolSubscription subscription = schoolSubscription
                .findBySchoolIdAndCurrentIsTrue(schoolId)
                .orElseThrow(() -> noSubscription(schoolId));

        //! step 2 - the plan it points at, for the name, the limits and the features
        // TODO: read plan
        PlanDefinition plan = planDefinition.findById(subscription.getPlanDefinitionDocsId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "The plan " + subscription.getSubscriptionNo() + " points at no longer "
                                + "exists."));

        return SubscriptionDetailResponse.fromSubscription(subscription, plan,
                subscriptionNote(subscription, plan));
    }

    //! endpoint 33 — the school's own billing screen -----------------------------------

    /**
     * #33 — what the school itself sees: its plan, what it costs, when it renews.
     *
     * <p><b>Not #27 with a different URL.</b> #27 is the platform read and shows everything;
     * this one leaves out the plan's list price, the gateway's customer reference and the
     * negotiated overrides. A school on a discount being shown a price it is not paying is
     * either a discount somebody then has to explain or an increase they will ring up about, and
     * the gateway's id for them is ours to hold. Two response types rather than one shared type
     * is what keeps that true when somebody adds a field later.
     *
     * <p>The school is never named in the URL — it comes from the tenant, so a caller cannot ask
     * about a school it does not belong to.
     */
    public MySubscriptionResponse getMySubscription(School school) {

        //! step 1 - the school's one current subscription
        // TODO: read subscription
        SchoolSubscription subscription = schoolSubscription
                .findBySchoolIdAndCurrentIsTrue(school.getId())
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                        "This school has no subscription."));

        //! step 2 - the plan it is on, for the name and the description
        // TODO: read plan
        PlanDefinition plan = planDefinition.findById(subscription.getPlanDefinitionDocsId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "The plan this subscription points at no longer exists."));

        return MySubscriptionResponse.fromSubscription(subscription, plan);
    }

    //* ---------------------------------------------------------------------------------

    /**
     * The plan version, if it is one a school can be put on today.
     *
     * <p>A draft is refused because its price is still being decided, and a retired one because
     * it was taken off the menu — putting a new school on either is the mistake this check
     * exists for.
     *
     * <p><b>{@code publiclyAvailable} is deliberately not checked.</b> A plan that is published
     * but off the public list is exactly a private quote, and this endpoint is how a private
     * quote gets sold.
     */
    private PlanDefinition loadSellablePlan(String code, Integer version) {
        String planCode = planValidator.normalizePlanCode(code);
        // TODO: read plan
        PlanDefinition plan = planDefinition.findByPlanCodeAndPlanVersion(planCode, version)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "No plan '" + planCode + "' version " + version + " exists."));

        if (plan.getStatus() != PlanStatus.ACTIVE) {
            // The advice differs by direction: a draft is one publish away from sellable, and a
            // retired plan is not coming back. Telling somebody to publish a retired plan sends
            // them to an endpoint that will refuse them.
            String advice = plan.getStatus() == PlanStatus.DRAFT
                    ? " Publish it first."
                    : " A retired plan cannot be sold again — use a plan that is still on the "
                            + "menu.";

            throw ApiException.conflict("PLAN_NOT_SELLABLE",
                    "'" + planCode + "' version " + version + " is " + plan.getStatus()
                            + ", so no school can be put on it." + advice);
        }

        Instant now = Instant.now();
        if (plan.getEffectiveUntil() != null && !plan.getEffectiveUntil().isAfter(now)) {
            throw ApiException.conflict("PLAN_NOT_SELLABLE",
                    "'" + planCode + "' version " + version + " stopped being sold on "
                            + plan.getEffectiveUntil() + ".");
        }
        if (plan.getEffectiveFrom() != null && plan.getEffectiveFrom().isAfter(now)) {
            throw ApiException.conflict("PLAN_NOT_SELLABLE",
                    "'" + planCode + "' version " + version + " does not go on sale until "
                            + plan.getEffectiveFrom() + ".");
        }
        return plan;
    }

    /**
     * When the first billing period ends.
     *
     * <p>Derived from the plan's cycle so a caller does not have to do calendar arithmetic that
     * the plan already implies — a yearly plan starting 1 April ends a year later, and getting
     * that wrong by a day means an invoice for the wrong period.
     *
     * <p><b>A {@code CUSTOM} cycle has no length</b>, so there is nothing to derive and the
     * caller must say. Guessing a month there would be a made-up contract term.
     */
    private Instant resolvePeriodEnd(Instant requested, Instant periodStart, BillingCycle cycle,
            String schoolTimeZone) {

        if (requested != null) {
            if (!requested.isAfter(periodStart)) {
                throw ApiException.badRequest("INVALID_BILLING_PERIOD",
                        "currentPeriodEnd (" + requested + ") must be after currentPeriodStart ("
                                + periodStart + ").");
            }
            return requested;
        }

        // Months and years need a calendar, and an Instant has none — Instant.plus(1, YEARS)
        // throws, because "a year" is not a fixed number of seconds. So the arithmetic happens
        // in the school's own zone and comes back as an instant.
        //
        // THE SCHOOL'S ZONE, not UTC, because a billing period is a pair of dates a person
        // reads: "your year runs to 31 March". Adding a year in UTC keeps the same UTC wall
        // clock and drifts the local one across a daylight-saving change, so a school would find
        // its period ending an hour earlier or later than it started.
        ZoneId zone = zoneOrUtc(schoolTimeZone);
        ZonedDateTime start = periodStart.atZone(zone);

        return switch (cycle) {
            case MONTHLY -> start.plusMonths(1).toInstant();
            case QUARTERLY -> start.plusMonths(3).toInstant();
            case HALF_YEARLY -> start.plusMonths(6).toInstant();
            case YEARLY -> start.plusYears(1).toInstant();
            case CUSTOM -> throw ApiException.badRequest("BILLING_PERIOD_END_REQUIRED",
                    "This plan bills on a CUSTOM cycle, which has no set length, so "
                            + "currentPeriodEnd has to be sent.");
        };
    }

    /**
     * The school's zone, or UTC if it has none we can read.
     *
     * <p>Falls back rather than failing: the zone was validated when the school was created and
     * again by #8, so an unreadable one here means data older than those checks — and refusing
     * to sell a subscription over it would be the wrong thing to break.
     */
    private ZoneId zoneOrUtc(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timeZone.trim());
        } catch (java.time.DateTimeException unreadable) {
            return ZoneOffset.UTC;
        }
    }

    /**
     * The 404 for a school with no subscription, once we know which 404 it is.
     *
     * <p>Only called when nothing was found, so the extra read is off the path that succeeds. It
     * is worth the trip: told only "not found", somebody checks the subscription code when the
     * school id was wrong all along.
     */
    private ApiException noSubscription(String schoolId) {
        // TODO: read school
        School school = schools.findById(schoolId).orElse(null);

        if (school == null) {
            return ApiException.notFound("SCHOOL_NOT_FOUND",
                    "No school found with id '" + schoolId + "'.");
        }

        return ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                "'" + school.getSchoolName() + "' has no subscription. Create one first.");
    }

    /**
     * Anything about this subscription worth saying out loud.
     *
     * <p>Each of these is a state the module can genuinely be in today, and each one would
     * otherwise be read wrongly off a single field.
     */
    private String subscriptionNote(SchoolSubscription subscription, PlanDefinition plan) {
        List<String> notes = new ArrayList<>();

        Instant end = subscription.getCurrentPeriodEnd();
        boolean live = subscription.getStatus() == SubscriptionStatus.ACTIVE
                || subscription.getStatus() == SubscriptionStatus.TRIAL;

        if (end != null && !end.isAfter(Instant.now()) && live) {
            notes.add("The period ended on " + end + " but the status still says "
                    + subscription.getStatus() + ". Nothing renews a subscription or marks one "
                    + "expired yet, so this has to be read as lapsed rather than paying.");
        }

        if (subscription.getStatus() == SubscriptionStatus.TRIAL) {
            notes.add("This is a trial. Activating it is what turns it into a paying "
                    + "subscription.");
        }

        if (plan.getStatus() == PlanStatus.RETIRED) {
            notes.add("'" + plan.getPlanCode() + "' version " + plan.getPlanVersion()
                    + " has been retired. This school keeps it — retiring only stops new sales "
                    + "— but it is no longer on the menu.");
        }

        return notes.isEmpty() ? null : String.join(" ", notes);
    }

    /**
     * The subscription named in the URL, or the school's current one.
     *
     * <p>See {@link #CURRENT_SUBSCRIPTION} for why the word is needed: a subscription number has
     * slashes in it and cannot be written in a path.
     */
    private SchoolSubscription findSubscription(School school, String schoolId,
            String subscriptionNo) {

        if (CURRENT_SUBSCRIPTION.equalsIgnoreCase(subscriptionNo)) {
            // TODO: read school subscription
            return schoolSubscription.findBySchoolIdAndCurrentIsTrue(schoolId)
                    .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                            "'" + school.getSchoolName() + "' has no subscription yet. Create "
                                    + "one first."));
        }

        // TODO: read school subscription
        return schoolSubscription.findBySchoolIdAndSubscriptionNo(schoolId, subscriptionNo)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                        "'" + school.getSchoolName() + "' has no subscription numbered '"
                                + subscriptionNo + "'. A subscription number contains slashes "
                                + "and cannot be written in a URL — use 'current'."));
    }

    /**
     * Refuses anything that is not a trial, and says what to do instead.
     *
     * <p>The advice differs by status because the way out differs: an ACTIVE subscription needs
     * nothing doing, a cancelled or expired one needs a new subscription, and a suspended one
     * needs the suspension lifted first. One message for all four would send three of them to
     * the wrong place.
     */
    private void requireTrial(SchoolSubscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.TRIAL) {
            return;
        }

        String advice = switch (subscription.getStatus()) {
            case ACTIVE -> " It is already paying, so there is nothing to do.";
            case PAST_DUE -> " It is already paying but has an unpaid bill. Take the payment "
                    + "rather than activating it again.";
            case SUSPENDED -> " Lift the suspension first.";
            case CANCELLED, EXPIRED -> " A finished subscription cannot be restarted. Create a "
                    + "new subscription for this school instead.";
            default -> "";
        };

        throw ApiException.conflict("SUBSCRIPTION_NOT_TRIAL",
                subscription.getSubscriptionNo() + " is " + subscription.getStatus()
                        + ", and only a TRIAL can be activated." + advice);
    }

    /** What the caller should know after a trial has been turned into a paying subscription. */
    private String activatedNextStep(School school, SchoolSubscription subscription) {
        String base = "Now paying, from " + subscription.getCurrentPeriodStart() + " to "
                + subscription.getCurrentPeriodEnd() + ".";

        String activation = switch (school.getStatus()) {
            case PROVISIONING -> " The school itself is still " + school.getStatus()
                    + " — activating the subscription does not activate the school.";
            case SUSPENDED -> " The school itself is still SUSPENDED — paying does not lift a "
                    + "suspension.";
            default -> "";
        };

        return base + activation + " No invoice has been raised: that is a separate step.";
    }

    /**
     * Writes the request onto the subscription, and returns the fields that actually moved.
     *
     * <p><b>"Changed" means different from what was stored.</b> A caller who resends the current
     * price has not edited anything, and recording that they did would fill the audit trail with
     * rows that explain nothing. So every field is compared before it is set.
     *
     * <p>{@code Objects.equals} throughout, because every field here is nullable — an override
     * that is not set is null, and null has to compare equal to itself.
     *
     * <p><b>{@code reasonForChanges} is not in the list.</b> It is written by the caller on every
     * edit, so it always "changed" — reporting it would put "reasonForChanges" in every history
     * row's field list and in every response note, next to the reason itself.
     */
    private List<String> applyEdit(SchoolSubscription subscription,
            SubscriptionUpdateRequest request) {

        List<String> changed = new ArrayList<>();

        if (request.status() != null && request.status() != subscription.getStatus()) {
            subscription.setStatus(request.status());
            changed.add("status");
        }

        if (request.billingCycle() != null
                && request.billingCycle() != subscription.getBillingCycle()) {
            subscription.setBillingCycle(request.billingCycle());
            changed.add("billingCycle");
        }

        if (request.currentPeriodStart() != null
                && !request.currentPeriodStart().equals(subscription.getCurrentPeriodStart())) {
            subscription.setCurrentPeriodStart(request.currentPeriodStart());
            changed.add("currentPeriodStart");
        }

        if (request.currentPeriodEnd() != null
                && !request.currentPeriodEnd().equals(subscription.getCurrentPeriodEnd())) {
            subscription.setCurrentPeriodEnd(request.currentPeriodEnd());
            changed.add("currentPeriodEnd");
        }

        if (request.autoRenew() != null
                && !request.autoRenew().equals(subscription.getAutoRenew())) {
            subscription.setAutoRenew(request.autoRenew());
            changed.add("autoRenew");
        }

        //! the two blocks. Sent means "replace both", so a null inside is a removal rather than
        //! an omission — which is the whole reason they are nested.
        if (request.limitOverrides() != null) {
            Long students = request.limitOverrides().maxStudentsOverride();
            Long users = request.limitOverrides().maxUsersOverride();
            validateOverride("maxStudentsOverride", students);
            validateOverride("maxUsersOverride", users);

            if (!Objects.equals(students, subscription.getMaxStudentsOverride())) {
                subscription.setMaxStudentsOverride(students);
                changed.add("maxStudentsOverride");
            }
            if (!Objects.equals(users, subscription.getMaxUsersOverride())) {
                subscription.setMaxUsersOverride(users);
                changed.add("maxUsersOverride");
            }
        }

        return changed;
    }

    /**
     * Which event this edit was.
     *
     * <p>An edit can move several things at once, and the row records one type, so it records the
     * most consequential: a status move changes whether the school gets the product at all, and
     * everything else is terms. {@code PLAN_CHANGED} is not written here — this endpoint cannot
     * move the plan, and #16 is what does.
     *
     * <p>The status types are the same ones the lifecycle endpoints will write, so a suspension
     * recorded through this endpoint and one recorded through #19 read identically in the
     * history — which is what somebody asking "when was this school suspended" needs.
     */
    private SubscriptionEventType eventTypeFor(SubscriptionStatus previousStatus,
            SubscriptionStatus newStatus) {

        if (newStatus == previousStatus) {
            return SubscriptionEventType.TERMS_CHANGED;
        }

        return switch (newStatus) {
            case TRIAL -> SubscriptionEventType.TRIAL_STARTED;
            case ACTIVE -> previousStatus == SubscriptionStatus.SUSPENDED
                    ? SubscriptionEventType.RESUMED
                    : SubscriptionEventType.ACTIVATED;
            case PAST_DUE -> SubscriptionEventType.PAYMENT_PAST_DUE;
            case SUSPENDED -> SubscriptionEventType.SUSPENDED;
            case CANCELLED -> SubscriptionEventType.CANCELLED;
            case EXPIRED -> SubscriptionEventType.EXPIRED;
        };
    }

    /**
     * What goes on the history row's reason.
     *
     * <p>The field list is always recorded, and the caller's own words are kept next to it when
     * they gave any. Months later "the price changed" is the question and "which fields moved" is
     * the answer — a reason of "renegotiated" alone does not say what was renegotiated.
     */
    private String auditReason(List<String> changed, String callerReason) {
        String fields = "Edited " + String.join(", ", changed) + ".";

        return callerReason == null || callerReason.isBlank()
                ? fields
                : fields + " " + callerReason.trim();
    }

    /** The plan a subscription points at, which must exist for the response to be complete. */
    private PlanDefinition loadPlanBehind(SchoolSubscription subscription) {
        // TODO: read plan
        return planDefinition.findById(subscription.getPlanDefinitionDocsId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "The plan " + subscription.getSubscriptionNo() + " points at no longer "
                                + "exists."));
    }

    /**
     * What the caller should know after an edit.
     *
     * <p>Says what moved, then anything the edit has left inconsistent. A cycle that no longer
     * matches the plan's is reported rather than corrected — billing a school monthly on a plan
     * that bills yearly is a real arrangement, and rewriting it would undo a deliberate change —
     * but left unsaid it would be found on an invoice instead.
     */
    private String editedNote(SchoolSubscription subscription, PlanDefinition plan,
            List<String> changed) {

        List<String> notes = new ArrayList<>();
        notes.add("Edited " + String.join(", ", changed) + ".");

        // Only the cycle. A currency mismatch is neither caused nor fixable here now that the
        // currency is #25's, so pointing it out on an edit that could not have done it would
        // send somebody looking for a field this endpoint does not have.
        if (plan.getBillingCycle() != subscription.getBillingCycle()) {
            notes.add("It bills " + subscription.getBillingCycle() + " while the plan bills "
                    + plan.getBillingCycle() + ".");
        }

        if (Boolean.FALSE.equals(subscription.getCurrent())) {
            notes.add("This is not the school's current subscription.");
        }

        String standing = subscriptionNote(subscription, plan);
        if (standing != null) {
            notes.add(standing);
        }

        notes.add("No invoice has been raised or credited: that is a separate step.");

        return String.join(" ", notes);
    }

    /** An override that lowers nothing and raises nothing is not an override. */
    private void validateOverride(String label, Long value) {
        if (value != null && value < 1) {
            throw ApiException.badRequest("LIMIT_TOO_LOW",
                    label + " must be at least 1 when it is sent. Received: " + value
                            + ". Omit it to use the plan's own limit.");
        }
    }

    /**
     * Takes the school live, if a subscription was the only thing it was waiting for.
     *
     * <p><b>Why this happens here at all.</b> A school with no subscription is a school nobody is
     * paying for, and the last step of onboarding was always "now activate it" — a second call
     * that could only ever succeed or say the setup was incomplete. Doing it here removes the
     * step without removing the checks.
     *
     * <p><b>The setup checks are NOT skipped.</b> Without a SCHOOL_ADMIN role and the number
     * sequences a live school fails on first use — core refuses activation over exactly this, and
     * so does this. The difference is that this does not fail the request: the subscription is
     * valid whether or not the school is ready to go live, and throwing here would roll back a
     * perfectly good subscription over a provisioning step. So it reports instead.
     *
     * <p><b>Only PROVISIONING moves.</b> A school already ACTIVE is left alone, and a SUSPENDED
     * one is certainly not quietly un-suspended by somebody buying a plan — that is what
     * reactivate is for, and it is a decision rather than a side effect.
     *
     * @return a sentence for the response saying what happened to the school's own status
     */
    private String activateIfReady(School school) {
        if (school.getStatus() != SchoolStatus.PROVISIONING) {
            return " The school itself is " + school.getStatus()
                    + ", which a subscription does not change.";
        }

        String notReady = schoolPlatform.whyNotReadyToActivate(school.getId());
        if (notReady != null) {
            return " The school is still PROVISIONING: " + notReady;
        }

        boolean firstActivation = school.getActivatedAt() == null;
        school.setStatus(SchoolStatus.ACTIVE);
        if (firstActivation) {
            school.setActivatedAt(Instant.now());
        }

        // TODO: update school
        schools.save(school);

        return " The school is now ACTIVE — a subscription was the last thing it needed.";
    }

    /** What the caller should know next, including what just happened to the school. */
    private String nextStepFor(SchoolSubscription subscription, boolean trial, String activation) {
        String base = trial
                ? "Trial started, running to " + subscription.getCurrentPeriodEnd() + "."
                : "Subscribed, and billed from " + subscription.getCurrentPeriodStart() + ".";

        return base + activation + " No invoice has been raised: that is a separate step.";
    }
}
