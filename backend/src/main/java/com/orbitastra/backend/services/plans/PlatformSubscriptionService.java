package com.orbitastra.backend.services.plans;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.subscription.MySubscriptionResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionDetailResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionCancelRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionCreateRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionPlanChangeRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionRenewRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionResumeRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionSuspendRequest;
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
    //! Endpoint 13 — a school's first subscription ------------------------------------

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
            //TODO: read the plan 
        PlanDefinition plan = loadSellablePlan(request.planCode(), request.planVersion());

        //! step 4 - work out the terms: the plan's, unless the caller overrode them
        //!
        //! The CYCLE comes first, because it is what decides the period. The plan's own cadence
        //! is the ordinary sale; a request that names one is selling the same entitlements on
        //! different terms, which is a negotiation like the price and the ceilings beside it.
        //! It lands on the subscription, so it changes what this school is billed and nothing
        //! about the plan.
        BillingCycle billingCycle = request.billingCycle() == null
                ? plan.getBillingCycle()
                : request.billingCycle();

        //! The start is REQUIRED — @NotNull on the request, so it is never null here. It used to
        //! default to today; a period start is the anchor the end is measured from, and on a
        //! yearly sale it fixes which day the school is billed on for as long as it stays, so it
        //! is somebody's decision rather than a convenience.
        //!
        //! A start in the past is refused rather than accepted: see the helper.
        validatePeriodStartIsTodayOrLater(request.currentPeriodStart(), school);

        Instant periodStart = request.currentPeriodStart();

        //! Derived from the cycle ABOVE, not the plan's. So a YEARLY plan sold as CUSTOM needs an
        //! end date and says so, and a CUSTOM plan sold as MONTHLY derives one and needs none —
        //! the refusal follows what the school is actually being billed on.
        Instant periodEnd = calculateSubscriptionPeriodEnd(request.currentPeriodEnd(), periodStart,
                billingCycle);

        BigDecimal contractedPrice = request.contractedPrice() == null
                ? plan.getListPrice()
                : planValidator.validatePrice("contractedPrice", request.contractedPrice());

        validateCapacityOverrideIsAtLeastOne("maxStudentsOverride", request.maxStudentsOverride());
        validateCapacityOverrideIsAtLeastOne("maxUsersOverride", request.maxUsersOverride());

        //! The capacity is written onto the subscription either way, copied from the plan when
        //! the caller named no figure of their own. So the document says what this school may
        //! use without anybody having to read the plan behind it to find out — and the day the
        //! plan's next version raises its ceiling, schools already sold keep what they bought.
        Long maxStudents = request.maxStudentsOverride() == null
                ? plan.getMaxStudents()
                : request.maxStudentsOverride();
        Long maxUsers = request.maxUsersOverride() == null
                ? plan.getMaxUsers()
                : request.maxUsersOverride();

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
                .billingCycle(billingCycle)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .autoRenew(request.autoRenew() == null ? Boolean.TRUE : request.autoRenew())
                .contractedPrice(contractedPrice)
                // The currency comes from the plan, never the caller: a subscription priced in a
                // different currency from the plan it points at is a mistake nobody would catch
                // until an invoice went out in the wrong money.
                .currencyCode(plan.getCurrencyCode())
                .maxStudentsOverride(maxStudents)
                .maxUsersOverride(maxUsers)
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
        String activation = activateSchoolIfSetupComplete(school);

        return SubscriptionResponse.fromSubscription(savedSubscription, plan,
                describeCreateOutcome(savedSubscription, trial, activation));
    }
    //! Endpoint 14 — edit what a school is contracted to ------------------------------

    /**
     * #14 — edits any of the terms of one subscription.
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
     * <p><b>{@code reason} is required, and lands in two places.</b> On the subscription as
     * {@code reasonForChanges}, so a screen can say why it looks the way it does without a second
     * query, and on the history row next to the fields that moved. The document keeps the latest;
     * history keeps all of them. Requiring it is what makes "why is this school's period ending
     * in December" answerable at all — every field here is something somebody is paying for.
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
        SchoolSubscription subscription = findSchoolSubscription(school, schoolId, subscriptionNo);

        //! step 4 - a cadence being SET names the dates it needs with it. Sending a cycle
        //! without them would leave this endpoint deriving a period from an anchor nobody
        //! restated: on the four fixed cycles the start is what the end is measured from, and
        //! CUSTOM has no length at all, so it needs both.
        //!
        //! Keyed on the cycle being SENT rather than on it changing. Resending the cadence a
        //! subscription is already on is still a statement about the period, and answering it
        //! differently depending on what happened to be stored would make the rule impossible
        //! to describe.
        if (request.billingCycle() != null) {
            if (request.currentPeriodStart() == null) {
                throw ApiException.badRequest("PERIOD_START_REQUIRED",
                        "currentPeriodStart has to be sent with billingCycle. A "
                                + request.billingCycle() + " period is measured from its start, "
                                + "so the cadence cannot be set without saying when the period "
                                + "it describes begins.");
            }

            if (request.billingCycle() == BillingCycle.CUSTOM
                    && request.currentPeriodEnd() == null) {
                throw ApiException.badRequest("BILLING_PERIOD_END_REQUIRED",
                        "currentPeriodEnd has to be sent with a CUSTOM billingCycle. CUSTOM has "
                                + "no length, so there is nothing to work the period out from — "
                                + "and the end date on record was derived from the cadence this "
                                + "subscription is leaving.");
            }
        }

        //! step 5 - a start being SET has to be today or later. The stored one is not checked:
        //! a subscription sold months ago has a start in the past by definition, and refusing to
        //! edit it would make every other field on this endpoint unreachable for a running
        //! subscription. Only a value on the request is a decision somebody is making now.
        validatePeriodStartIsTodayOrLater(request.currentPeriodStart(), school);

        //! step 6 - apply the edit, keeping a list of what actually moved. The list is what the
        //! history row and the response are built from, so "changed" means "different from what
        //! was stored", not "was mentioned in the request".
        SubscriptionStatus previousStatus = subscription.getStatus();
        List<String> changed = applySubscriptionEdits(subscription, request);

        if (changed.isEmpty()) {
            // TODO: read plan
            PlanDefinition unchangedPlan = loadPlanBehindSubscription(subscription);
            return SubscriptionDetailResponse.fromSubscription(subscription, unchangedPlan,
                    "Nothing changed: every field sent already held that value. No history row "
                            + "was written.");
        }

        //! step 7 - the cycle decides the period, the same way it does on a sale. A cadence that
        //! moved leaves the stored end date describing a period nobody is on any more: an end
        //! derived as "start + 365" is not the end of a MONTHLY period, so keeping it would bill
        //! the school for a year while the document says it pays monthly.
        //!
        //! An explicit currentPeriodEnd always wins, exactly as on #13 — this only fills in the
        //! date nobody sent. The UI disables that box for the four fixed cycles precisely because
        //! the cycle already decides it.
        boolean cycleMoved = changed.contains("billingCycle");
        boolean startMoved = changed.contains("currentPeriodStart");

        //! A CUSTOM cadence never reaches here without an end — step 4 requires one with it —
        //! so the only case left is a fixed cadence whose start or cycle moved. A CUSTOM
        //! subscription whose start moves alone keeps its agreed end: that date was somebody's
        //! decision rather than a derivation, and step 8 checks it is still after the new start.
        if (request.currentPeriodEnd() == null && (cycleMoved || startMoved)
                && subscription.getBillingCycle() != BillingCycle.CUSTOM) {

            Instant derivedEnd = calculateSubscriptionPeriodEnd(null,
                    subscription.getCurrentPeriodStart(), subscription.getBillingCycle());

            if (!derivedEnd.equals(subscription.getCurrentPeriodEnd())) {
                subscription.setCurrentPeriodEnd(derivedEnd);
                changed.add("currentPeriodEnd");
            }
        }

        //! step 8 - the reason goes onto the document as well as the history row. Written only
        //! now, because an edit that changed nothing has nothing to explain. No null check: the
        //! request has @NotBlank on it, so an unexplained edit never reaches here.
        subscription.setReasonForChanges(request.reason().trim());

        //! step 9 - the period has to still make sense after the edit, whichever end moved
        if (!subscription.getCurrentPeriodEnd().isAfter(subscription.getCurrentPeriodStart())) {
            throw ApiException.badRequest("INVALID_BILLING_PERIOD",
                    "currentPeriodEnd (" + subscription.getCurrentPeriodEnd() + ") must be after "
                            + "currentPeriodStart (" + subscription.getCurrentPeriodStart()
                            + "). Editing one end of a period is checked against the other.");
        }

        // TODO: update school subscription
        SchoolSubscription saved = schoolSubscription.save(subscription);

        //! step 10 - one history row for the whole edit, in this same transaction
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(chooseHistoryEventType(previousStatus, saved.getStatus()))
                .previousStatus(previousStatus)
                .newStatus(saved.getStatus())
                // The plan cannot move here, so there is no previous plan to record — both ids
                // are the one it is still on. #16 is what writes a plan change.
                .newPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason(buildHistoryReason(changed, request.reason()))
                .performedByDocsId(null)
                .effectiveAt(Instant.now())
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 11 - the plan is read only so the response can carry its features and limits
        // TODO: read plan
        PlanDefinition plan = loadPlanBehindSubscription(saved);

        //! step 12 - the note is two answers joined here rather than by one helper calling the
        //! other: what this edit did, and anything standing about the subscription that a
        //! reader needs whether or not it was edited.
        List<String> note = new ArrayList<>();
        note.add(describeEditOutcome(saved, plan, changed));

        String standing = describeSubscriptionState(saved, plan);
        if (standing != null) {
            note.add(standing);
        }

        note.add("No invoice has been raised or credited: that is a separate step.");

        return SubscriptionDetailResponse.fromSubscription(saved, plan,
                String.join(" ", note));
    }
    //! Endpoint 16 — move a school onto a different plan ------------------------------

    /**
     * #16 — moves a school onto a different plan, or a newer version of its own.
     *
     * <p><b>What #14 deliberately cannot do.</b> #14 edits the terms of the plan a school is
     * already on; this changes which plan that is, and with it what the school is entitled to,
     * what it costs and how often it is billed. Keeping them apart is what stops "push the trial
     * out a fortnight" and "move them to Enterprise" looking like the same request.
     *
     * <p><b>It writes two rows, and does not edit one.</b> The row the school is leaving is
     * closed — {@code current = false}, and its period trimmed to today, because that is the
     * period it actually served — and a new row is inserted for the plan it moves onto, with a
     * {@code subscriptionNo} of its own. So {@code school_subscriptions} keeps one row per plan
     * period rather than one row per school, and "what was this school on in March" is answerable
     * from the collection instead of only from the history.
     *
     * <p>The old row's <b>status is not touched</b>. It did not expire and it was not cancelled —
     * it was superseded, and writing either of the other two words would put something false in
     * the record.
     *
     * <p><b>No status is refused, and the new row is always {@code ACTIVE}.</b> A plan change is
     * somebody buying this school a plan, so the row it lands on has to be usable — and it now
     * agrees with what this endpoint does to the school itself. A trial converts this way; a
     * cancelled subscription comes back this way, with the finished row's dates left exactly as
     * they were, because it really did stop then. {@code current} is the field that says which row is live, which is why every
     * read of "the school's subscription" goes through
     * {@code findBySchoolIdAndCurrentIsTrue}.
     *
     * <p><b>The plan changes immediately, and there is no option not to.</b> A subscription holds
     * one plan, not a current one and a pending one, so a change scheduled for the next period
     * would have nowhere to live — and moving the pointer now while calling it next period would
     * hand the school its new entitlements early.
     *
     * <p><b>What the request does choose is when the new billing PERIOD begins</b>, through the
     * required {@code currentPeriodStart}. Today is the ordinary answer and reproduces the old
     * behaviour exactly. A later date does not delay the entitlements — only the period — and the
     * row being left has its end moved to that same instant, so the two periods meet.
     *
     * <p><b>It asks nothing about the money already paid, and moves none.</b> The school is
     * part-way through a period it has paid for, and nothing here charges, credits or refunds any
     * of it — because nothing in this codebase raises an invoice at all:
     * {@code subscription_invoices} has no writer and #17 is not built. The response says so
     * rather than leaving somebody to assume a charge went out.
     *
     * <p>Deciding what <i>should</i> happen to that money is a commercial question, and it
     * belongs with whatever raises the invoice rather than with the request that moves the plan.
     * {@code controllers/plans/README.md} keeps it as an open question.
     *
     * <p><b>The new plan is the starting point for everything negotiable.</b> Price and both
     * capacity ceilings come from it unless the request names them, exactly as #13 does on a
     * sale — so a school that had negotiated a ceiling on its old plan does not keep it
     * automatically. A ceiling is agreed against a particular plan, and moving to a different one
     * means the terms are being renegotiated whether or not anybody says so; re-stating them here
     * is what makes the new arrangement somebody's decision rather than this method's.
     *
     * <p><b>{@code autoRenew} is the exception, and absent leaves it alone.</b> A plan has no
     * opinion about renewal — it is the school's standing instruction — so defaulting it to
     * {@code true} the way a sale does would switch it back on for the one school that had asked
     * for it off.
     *
     * <p><b>Nothing checks whether a downgrade puts the school over its new ceiling</b>, because
     * nothing counts students yet. The response says so rather than implying the move was safe.
     *
     * <p><b>It moves the school's own status, which is the one side effect it has outside
     * {@code school_subscriptions}.</b> Only a school that is still running may change plan —
     * PROVISIONING, ACTIVE or SUSPENDED — and all three come out ACTIVE, because a school paying
     * for a plan should be able to use it. The four wind-down states (OFFBOARDING, CLOSED,
     * DELETION_PENDING, DELETED) are refused with
     * {@code 409 SCHOOL_NOT_PLAN_CHANGEABLE}: selling a different plan to a school that is
     * leaving, and taking it ACTIVE on the way, would reverse a wind-down as a side effect.
     *
     * <p>Note that this un-suspends, where a sale does not: #13 leaves a SUSPENDED school
     * suspended, on the argument that lifting a suspension is a decision rather than a side
     * effect of buying a plan. Here it is the opposite — a suspension is ordinarily for
     * non-payment, and a school being moved onto a new plan has generally sorted that out, so
     * leaving it locked out would bill it for something it cannot reach. The response always
     * says what happened to the school's status, so it is never a silent change.
     */
    @Transactional
    public SubscriptionDetailResponse changePlan(String schoolId, String subscriptionNo,
            SubscriptionPlanChangeRequest request) {

        //! step 1 - the school has to exist, and be one that is still running rather than one
        //! being wound down. PROVISIONING, ACTIVE and SUSPENDED can all change plan; the four
        //! shutdown states cannot. Named as an allow-list so a status added to the enum later
        //! is refused until somebody decides it should be allowed.
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! An allow-list, not a deny-list: the three running states are named and everything
        //! else is refused, so a status added to SchoolStatus later cannot quietly become one
        //! in which plans may be changed. A conflict rather than a 400 — nothing about the
        //! request is malformed, and the same request against a running school would work.
        //!
        //! OFFBOARDING is refused HERE and accepted by #13 and #14. A school being wound down
        //! still has a subscription that may need correcting, and refusing to edit one would
        //! leave a wrong record un-fixable — but moving it onto a DIFFERENT plan sells to a
        //! customer who is leaving, and step 14 takes the school ACTIVE as it goes, which would
        //! reverse the wind-down as a side effect.
        boolean schoolIsStillRunning = school.getStatus() == SchoolStatus.PROVISIONING
                || school.getStatus() == SchoolStatus.ACTIVE
                || school.getStatus() == SchoolStatus.SUSPENDED;

        if (!schoolIsStillRunning) {
            throw ApiException.conflict("SCHOOL_NOT_PLAN_CHANGEABLE",
                    "'" + school.getSchoolName() + "' is " + school.getStatus() + ", so its plan "
                            + "cannot be changed. Only a PROVISIONING, ACTIVE or SUSPENDED "
                            + "school can be moved to another plan.");
        }

        //! step 2 - the subscription named in the URL, or the one they are on now
        SchoolSubscription subscription = findSchoolSubscription(school, schoolId, subscriptionNo);

        //! step 3 - no status is refused, and a finished one is the interesting case: moving a
        //! CANCELLED or EXPIRED subscription onto a plan is how a school comes back. This used
        //! to be refused on the grounds that there was "nothing to move", which mistook what
        //! this endpoint does — it does not edit the old row, it retires it and opens a new one,
        //! so the state the old row ended in does not constrain the new one at all.
        //!
        //! The row that gets opened is ACTIVE whatever the old one was — see step 12. Nothing
        //! else keys on this flag: autoRenew is carried across untouched like every other
        //! standing instruction, and the closed row's dates are worked out in step 9 from the
        //! dates themselves, which are the only thing that knows whether the row was serving.
        //! All it does now is let the note point out that a cancellation left autoRenew off.
        boolean revivingFinished = subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED;

        //! step 4 - the plan being left, read before anything moves so the response and the
        //! history row can both name it
        // TODO: read plan
        PlanDefinition previousPlan = loadPlanBehindSubscription(subscription);

        //! step 5 - the plan being moved to, which has to be one we can sell today
        PlanDefinition newPlan = loadSellablePlan(request.planCode(), request.planVersion());

        if (newPlan.getId().equals(previousPlan.getId())) {
            throw ApiException.conflict("PLAN_UNCHANGED",
                    "'" + newPlan.getPlanCode() + "' version " + newPlan.getPlanVersion()
                            + " is the plan this subscription is already on. To change its terms "
                            + "rather than its plan, use the edit endpoint.");
        }

        //! step 6 - work out the price. The new plan's, unless the caller named one: a discount
        //! is agreed against a plan at a price, and this is a different plan at a different
        //! price, so carrying the old figure over silently would invent a deal nobody made.
        BigDecimal newPrice = request.contractedPrice() == null
                ? newPlan.getListPrice()
                : planValidator.validatePrice("contractedPrice", request.contractedPrice());

        //! step 7 - work out the ceilings: the caller's, or the new plan's own. The plan being
        //! left does not come into it — a ceiling is agreed against a particular plan, so moving
        //! to a different one means the figure is agreed again, and this request is where it is
        //! said. The same rule #13 uses on a sale.
        validateCapacityOverrideIsAtLeastOne("maxStudentsOverride", request.maxStudentsOverride());
        validateCapacityOverrideIsAtLeastOne("maxUsersOverride", request.maxUsersOverride());

        Long newMaxStudents = request.maxStudentsOverride() == null
                ? newPlan.getMaxStudents()
                : request.maxStudentsOverride();
        Long newMaxUsers = request.maxUsersOverride() == null
                ? newPlan.getMaxUsers()
                : request.maxUsersOverride();

        //! step 8 - the cadence and the period. The cycle is the new plan's unless this request
        //! names one, exactly as on a sale: putting a school on a plan at a cadence the plan is
        //! not listed at is a negotiation, and it lands on the subscription rather than the plan.
        BillingCycle billingCycle = request.billingCycle() == null
                ? newPlan.getBillingCycle()
                : request.billingCycle();

        //! The start is REQUIRED — @NotNull on the request — and has to be today or later. It is
        //! the anchor the new end is measured from AND the instant the row being left stops
        //! serving, so it decides two dates rather than one.
        validatePeriodStartIsTodayOrLater(request.currentPeriodStart(), school);

        Instant periodStart = request.currentPeriodStart();

        //! Derived from the cadence ABOVE, not the plan's, so the refusal follows what the school
        //! is actually billed on: a YEARLY plan billed CUSTOM needs an end date and says so.
        Instant periodEnd = calculateSubscriptionPeriodEnd(request.currentPeriodEnd(),
                periodStart, billingCycle);

        if (billingCycle == BillingCycle.CUSTOM && request.currentPeriodEnd() == null) {
            throw ApiException.badRequest("BILLING_PERIOD_END_REQUIRED",
                    "currentPeriodEnd has to be sent when the new cadence is CUSTOM, which has "
                            + "no length to work the period out from.");
        }

        //! step 9 - close the row the school is leaving. It stops being the current one, and its
        //! period ends exactly where the new one begins, so the two meet and the school is never
        //! on neither. Ordinarily that TRIMS it — the old end date would otherwise claim months
        //! the school was not on that plan — and for a start dated later it extends it instead,
        //! which is the same statement: the old plan serves until the new one takes over.
        //!
        //! Its status is deliberately NOT touched. It did not expire and it was not cancelled;
        //! it was superseded, and inventing one of the other two would put a wrong word in the
        //! record. `current = false` is what says it is history.
        String previousPlanId = subscription.getPlanDefinitionDocsId();
        String previousSubscriptionNo = subscription.getSubscriptionNo();
        SubscriptionStatus previousSubscriptionStatus = subscription.getStatus();
        Instant previousPeriodEnd = subscription.getCurrentPeriodEnd();

        subscription.setCurrent(false);

        //! ITS END NEVER MOVES FORWARD. A closed period only ever shrinks: this row served
        //! until the new one starts, or until it stopped on its own, whichever came first.
        //!
        //! One rule rather than a branch on the status, and it is the dates that know the
        //! answer. A live row's end is in the future, so it is trimmed to the handover — leaving
        //! it would claim the school was on this plan for months it was not. A row that already
        //! stopped keeps the date it stopped on, because moving that forward would claim it
        //! covered a gap the school was on nothing for.
        //!
        //! Keying on the status instead got the middle case wrong: a SCHEDULED cancellation is
        //! CANCELLED with an end still in the future, and it really was serving until now — so
        //! it does need trimming, and skipping it left two rows claiming the same days.
        if (previousPeriodEnd == null || previousPeriodEnd.isAfter(periodStart)) {
            subscription.setCurrentPeriodEnd(periodStart);
        }
        subscription.setReasonForChanges("Superseded by a plan change to '"
                + newPlan.getPlanCode() + "' version " + newPlan.getPlanVersion() + ". "
                + request.reason().trim());

        //! step 10 - written BEFORE the new row is inserted, and that order matters: the unique
        //! partial index on {schoolId, current} allows one current row per school, so inserting
        //! the new one first would collide with the old one still claiming to be current.
        // TODO: update school subscription
        schoolSubscription.save(subscription);

        //! step 11 - a number of its own. Two rows for one school cannot share a subscriptionNo:
        //! a unique index on {schoolId, subscriptionNo} says so, and an invoice pointing at a
        //! number that matches two records would be unanswerable.
        String subscriptionNumber = numberSequences.next(schoolId, NumberSequenceType.SUBSCRIPTION,
                "SUB/{YYYY}/{MM}/");

        //! step 12 - build the row the school moves onto. What carries across is only what a
        //! subscription needs to be a subscription; everything negotiable came from the request
        //! or the new plan in steps 6 and 7.
        SchoolSubscription moved = SchoolSubscription.builder()
                .schoolId(schoolId)
                .subscriptionNo(subscriptionNumber)
                .planDefinitionDocsId(newPlan.getId())
                .planVersion(newPlan.getPlanVersion())
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(billingCycle)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .autoRenew(request.autoRenew() == null
                        ? subscription.getAutoRenew()
                        : request.autoRenew())
                .contractedPrice(newPrice)
                .currencyCode(newPlan.getCurrencyCode())
                .maxStudentsOverride(newMaxStudents)
                .maxUsersOverride(newMaxUsers)
                // The payment provider's handle belongs to the school, not to the plan it is on.
                .billingCustomerReference(subscription.getBillingCustomerReference())
                .reasonForChanges(request.reason().trim())
                .current(true)
                .build();

        // TODO: insert school subscription
        SchoolSubscription saved = schoolSubscription.save(moved);

        //! step 13 - one history row, against the row the school moved ONTO, carrying both plan
        //! ids so the move reads in one line. The old subscriptionNo goes in the reason because
        //! the history document has no field for it, and without it the two rows are only
        //! findable by knowing to query on schoolId.
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(SubscriptionEventType.PLAN_CHANGED)
                // These differ on a revival, and are equal on every other plan change. Reading
                // the old row's status off `previousStatus` is the only way a history row says
                // a school came back from cancelled.
                .previousStatus(previousSubscriptionStatus)
                .newStatus(saved.getStatus())
                .previousPlanDefinitionDocsId(previousPlanId)
                .newPlanDefinitionDocsId(newPlan.getId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason("Moved from '" + previousPlan.getPlanCode() + "' version "
                        + previousPlan.getPlanVersion() + " to '" + newPlan.getPlanCode()
                        + "' version " + newPlan.getPlanVersion() + ", immediately. "
                        + previousSubscriptionNo + " was closed and "
                        + saved.getSubscriptionNo() + " opened. " + request.reason().trim())
                .performedByDocsId(null)
                .effectiveAt(periodStart)
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 14 - the school itself goes ACTIVE. A plan change is a school paying for
        //! something, and a school that is paying should be able to use what it pays for, so
        //! whichever of the three running states it was in, it comes out of this ACTIVE.
        //!
        //! This deliberately UN-SUSPENDS, where #13 deliberately does not: buying a plan is not
        //! a reason to lift a suspension, but a suspension is ordinarily for non-payment and a
        //! school being moved onto a new plan has generally sorted that out — leaving it locked
        //! out would bill it for something it cannot reach. The note says so, so it is never
        //! silent.
        //!
        //! It does NOT re-run the provisioning checks, unlike #13. A PROVISIONING school
        //! reaching here already has a subscription, so #13 has run them and either activated
        //! the school or said why not; refusing again would block a plan change over an
        //! onboarding task that has nothing to do with the plan. The note reports an unfinished
        //! setup instead, so putting such a school live is visible rather than silent.
        SchoolStatus schoolStatusBefore = school.getStatus();
        String schoolNote;

        if (schoolStatusBefore == SchoolStatus.ACTIVE) {
            schoolNote = "The school was already ACTIVE and stays that way.";
        } else {
            boolean firstActivation = school.getActivatedAt() == null;
            school.setStatus(SchoolStatus.ACTIVE);
            if (firstActivation) {
                school.setActivatedAt(Instant.now());
            }

            // TODO: update school — status to ACTIVE
            schools.save(school);

            if (schoolStatusBefore == SchoolStatus.SUSPENDED) {
                schoolNote = "The school was SUSPENDED and is now ACTIVE — the plan change "
                        + "lifted it, so check that whatever the suspension was for has "
                        + "actually been resolved.";
            } else {
                String notReady = schoolPlatform.whyNotReadyToActivate(school.getId());
                schoolNote = notReady == null
                        ? "The school was PROVISIONING and is now ACTIVE."
                        : "The school was PROVISIONING and is now ACTIVE, but its setup is not "
                                + "finished: " + notReady;
            }
        }

        //! step 15 - the note is assembled here rather than by one helper calling another: what
        //! the move did, what it deliberately did not do to the money, what happened to the
        //! school, and anything standing about the subscription a reader needs either way.
        List<String> note = new ArrayList<>();
        note.add(describePlanMove(previousPlan, newPlan, saved, previousSubscriptionNo));

        if (previousSubscriptionStatus != SubscriptionStatus.ACTIVE) {
            note.add("It was " + previousSubscriptionStatus + " and the new row is ACTIVE: a "
                    + "plan change is somebody buying this school a plan, so the row it lands on "
                    + "is one the school can use.");
        }

        if (previousPeriodEnd != null && !previousPeriodEnd.isAfter(periodStart)) {
            note.add(previousSubscriptionNo + " kept its own end of " + previousPeriodEnd
                    + ", because it had already stopped by then — a closed period is only ever "
                    + "shortened, never stretched forward. The gap between that and "
                    + periodStart + " is time this school was on nothing.");
        }

        //! autoRenew is the school's standing instruction and is carried across untouched, so a
        //! revival inherits whatever the cancellation left — #21 turns it off on its way out.
        //! Nothing enforces the flag, so this costs the school nothing; what it does do is make
        //! its own billing view (#33) say a subscription somebody has just bought does not renew.
        //! Said here rather than quietly corrected, because overriding a standing instruction is
        //! the caller's decision to make.
        if (revivingFinished && Boolean.FALSE.equals(saved.getAutoRenew())) {
            note.add("autoRenew is still off: the cancellation turned it off and this request "
                    + "did not name it, so it carried across. Nothing acts on the flag, but the "
                    + "school's own billing view reads it — send autoRenew: true if this "
                    + "subscription should say it renews.");
        }

        note.add(schoolNote);

        String standing = describeSubscriptionState(saved, newPlan);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, newPlan,
                String.join(" ", note));
    }

    //! Endpoint 17 — start the next billing period ------------------------------------

    /**
     * #17 — renews a subscription into its next billing period, on the same terms.
     *
     * <p><b>The ordinary renewal sends no body, and one field exists for the case that cannot.</b>
     * A renewal is the same plan at the same price for the next period: the plan, the version, the
     * price, the currency, both capacity ceilings, the cycle, {@code autoRenew} and the billing
     * customer reference all carry across untouched. Anything that could change one of those would
     * make this a change rather than a renewal, and changes have their own endpoints — #14 for the
     * terms, #16 for the plan.
     *
     * <p>The exception is {@code currentPeriodEnd}, and only because a {@code CUSTOM} cycle has no
     * length: there is nothing to derive, so the caller has to say when the next period ends.
     * Required on {@code CUSTOM}, an override on the four fixed cycles, and omitted — with no body
     * at all — for every ordinary renewal.
     *
     * <p><b>It writes two rows, the same way #16 does.</b> A renewal is a new billing period, and
     * {@code school_subscriptions} holds one document per period rather than one per school — so
     * the period that just ended is closed ({@code current = false}) and a new row is inserted
     * with a {@code subscriptionNo} of its own. That is what makes "what was this school paying
     * in March, and for which period" answerable from the collection.
     *
     * <p>The closed row keeps its status. It did not expire and was not cancelled — it ran its
     * course and was renewed, and {@code current} is the field that says which row is live.
     *
     * <p><b>The new period starts where the old one ended, not today.</b> Contiguous, so there is
     * no gap the school was live but unbilled for and no overlap it was billed twice for. This is
     * why renewal is refused before the period has actually ended: starting the next period early
     * would leave the school's current row with a period that has not begun, and every read would
     * have to explain it.
     *
     * <h2>What it refuses, and why each one</h2>
     *
     * <pre>
     * plan retired or withdrawn   -> 409 PLAN_NOT_RENEWABLE     nothing to re-commit to
     * period still running        -> 409 PERIOD_NOT_ENDED       there is no next period yet
     * TRIAL                       -> 409 SUBSCRIPTION_NOT_RENEWABLE  a trial has no next period
     * SUSPENDED                   -> 409 SUBSCRIPTION_NOT_RENEWABLE  billing blocked access
     * CANCELLED                   -> 409 SUBSCRIPTION_NOT_RENEWABLE  deliberately ended
     * CUSTOM, no end date sent    -> 400 BILLING_PERIOD_END_REQUIRED  say when it ends
     * an end date not after start -> 400 INVALID_BILLING_PERIOD   it would end before it began
     * a school being wound down   -> 409 SCHOOL_NOT_RENEWABLE    do not bill a school that is going
     * </pre>
     *
     * <p><b>The plan has to still be current, and a retired one is refused.</b> A renewal commits
     * the school to the same plan for another period, so a plan that is no longer sold — retired,
     * back to DRAFT, or past its {@code effectiveUntil} — is not something to re-commit to
     * silently. {@code 409 PLAN_NOT_RENEWABLE} says so and points at #16, because moving the
     * school onto a plan that is still current is the only fix. A private plan is not affected:
     * {@code publiclyAvailable} is a quote, not a state, and a school on one renews like any
     * other.
     *
     * <p><b>{@code autoRenew} is not checked, and does not refuse anything.</b> Nothing calls
     * this endpoint on a schedule, so every renewal is an operator deciding to renew this school
     * now — and refusing that because of a flag would mean editing the flag first just to get
     * past this endpoint. The flag is still carried onto the new row, and the school's own view
     * still tells it the subscription does not renew automatically; what does not exist is
     * anything that renews on its own for the flag to govern.
     *
     * <p><b>ACTIVE, PAST_DUE and EXPIRED renew, and all three come out ACTIVE.</b> EXPIRED is the
     * case this endpoint exists to repair — a period ran out because nothing renewed it — and
     * leaving it EXPIRED after starting a new period would contradict the row's own dates.
     * PAST_DUE renews because an operator calling this by hand is saying the period should start;
     * the note says the outstanding payment is not thereby settled, because nothing here settles
     * it.
     *
     * <p><b>No invoice is raised, and that is the one thing this endpoint is missing.</b> The
     * table for #17 names {@code subscription_invoices}, and it is deliberately not written: no
     * repository exists for it, and the fields it would need — {@code subTotal},
     * {@code taxAmount}, {@code dueDate} — are commercial decisions rather than something this
     * method can derive from a plan's price. So this moves the billing period and records the
     * renewal; it does not charge for it. The response says so rather than letting a caller
     * assume money moved.
     *
     * <p><b>It does not touch the school's own status</b>, unlike #16. A renewal is the
     * continuation of an arrangement rather than a new one, so there is nothing about it that
     * should take a school live or lift a suspension.
     */
    @Transactional
    public SubscriptionDetailResponse renewSubscription(String schoolId, String subscriptionNo,
            SubscriptionRenewRequest request) {

        //! step 1 - the school has to exist, and be one worth billing for another period. The
        //! same allow-list #16 uses: starting a new billing period for a school that is closing
        //! bills a customer who is leaving.
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        boolean schoolIsStillRunning = school.getStatus() == SchoolStatus.PROVISIONING
                || school.getStatus() == SchoolStatus.ACTIVE
                || school.getStatus() == SchoolStatus.SUSPENDED;

        if (!schoolIsStillRunning) {
            throw ApiException.conflict("SCHOOL_NOT_RENEWABLE",
                    "'" + school.getSchoolName() + "' is " + school.getStatus() + ", so its "
                            + "subscription cannot be renewed. Only a PROVISIONING, ACTIVE or "
                            + "SUSPENDED school can be billed for another period.");
        }

        //! step 2 - the subscription named in the URL, or the one they are on now
        SchoolSubscription subscription = findSchoolSubscription(school, schoolId, subscriptionNo);

        //! step 3 - only three statuses have a next period. A TRIAL does not renew into one:
        //! nobody has agreed what it costs, so extending it is #14 and converting it is #16. A
        //! SUSPENDED subscription would be billed for a period the school cannot use, and a
        //! CANCELLED one was deliberately ended — renewing either would undo a decision.
        SubscriptionStatus statusBefore = subscription.getStatus();

        boolean renewable = statusBefore == SubscriptionStatus.ACTIVE
                || statusBefore == SubscriptionStatus.PAST_DUE
                || statusBefore == SubscriptionStatus.EXPIRED;

        if (!renewable) {
            throw ApiException.conflict("SUBSCRIPTION_NOT_RENEWABLE",
                    subscription.getSubscriptionNo() + " is " + statusBefore + ", so it has no "
                            + "next billing period. Only ACTIVE, PAST_DUE and EXPIRED "
                            + "subscriptions renew — a trial is extended with the edit endpoint "
                            + "and converted by changing its plan.");
        }

        //! step 4 - the plan it is on, read before anything moves so the response can name it.
        // TODO: read plan
        PlanDefinition plan = loadPlanBehindSubscription(subscription);

        //! step 5 - the plan has to still be one a school can be on for another period. A
        //! renewal commits the school to the SAME plan again, so a plan that is no longer being
        //! sold is not something to re-commit to by default — the fix is to move the school onto
        //! a plan that is, which is #16.
        //!
        //! Deliberately NOT loadSellablePlan: that one is for choosing a plan to sell, and its
        //! advice ("publish it first", "pick one still on the menu") is wrong here. The school is
        //! already on this plan; the question is whether to run it for another period, and the
        //! answer when the plan has gone is always the same one endpoint.
        //!
        //! publiclyAvailable is deliberately not consulted. A private plan is a negotiated quote,
        //! not an invalid plan, and a school on one renews like any other.
        Instant checkedAt = Instant.now();

        boolean planIsRetired = plan.getStatus() == PlanStatus.RETIRED;
        boolean planIsUnpublished = plan.getStatus() == PlanStatus.DRAFT;
        boolean planWindowClosed = plan.getEffectiveUntil() != null
                && !plan.getEffectiveUntil().isAfter(checkedAt);
        boolean planWindowNotOpen = plan.getEffectiveFrom() != null
                && plan.getEffectiveFrom().isAfter(checkedAt);

        if (planIsRetired || planIsUnpublished || planWindowClosed || planWindowNotOpen) {
            String because;
            if (planIsRetired) {
                because = "has been retired";
            } else if (planIsUnpublished) {
                because = "is back to DRAFT with its terms unsettled";
            } else if (planWindowClosed) {
                because = "stopped being sold on " + plan.getEffectiveUntil();
            } else {
                because = "does not go on sale until " + plan.getEffectiveFrom();
            }

            throw ApiException.conflict("PLAN_NOT_RENEWABLE",
                    "'" + plan.getPlanCode() + "' version " + plan.getPlanVersion() + " "
                            + because + ", so " + subscription.getSubscriptionNo() + " cannot be "
                            + "renewed onto it for another period. Move this school to a current "
                            + "plan with the change-plan endpoint instead.");
        }

        //! step 6 - a CUSTOM cycle has no length, so the caller has to say when the next period
        //! ends. This is the ONLY thing this endpoint accepts a body for, and the only cycle that
        //! needs one: the other four derive their own end from the days in the cycle.
        //!
        //! Refused rather than guessed. A custom contract runs to a date somebody agreed, and
        //! inventing one — a year, or the length of the last period — would put a date in a
        //! billing record that nobody signed off.
        Instant requestedPeriodEnd = request == null ? null : request.currentPeriodEnd();

        if (subscription.getBillingCycle() == BillingCycle.CUSTOM && requestedPeriodEnd == null) {
            throw ApiException.badRequest("BILLING_PERIOD_END_REQUIRED",
                    subscription.getSubscriptionNo() + " bills on a CUSTOM cycle, which has no "
                            + "set length, so currentPeriodEnd has to be sent to say when the "
                            + "next period ends.");
        }

        //! step 7 - there is no next period until this one has finished. Renewing early would
        //! insert a current row whose period starts in the future, leaving every read of "what
        //! is this school on" to explain a subscription that has not begun.
        Instant previousPeriodEnd = subscription.getCurrentPeriodEnd();

        if (previousPeriodEnd == null || previousPeriodEnd.isAfter(Instant.now())) {
            throw ApiException.conflict("PERIOD_NOT_ENDED",
                    subscription.getSubscriptionNo() + " runs to " + previousPeriodEnd
                            + ", which has not passed yet, so there is no next period to start. "
                            + "To move that date, use the edit endpoint.");
        }

        //! step 8 - the next period starts exactly where the last one ended, so the two are
        //! contiguous: no day the school was live but unbilled, and none it was billed twice for.
        //! The cycle is the SUBSCRIPTION's, not the plan's — #14 can have changed it, and a
        //! renewal renews what the school is actually on.
        //! The caller's date wins where one was sent — required on CUSTOM, an override on the
        //! rest — and is checked against the new period's start, so a renewal cannot be made to
        //! end before it began.
        Instant periodStart = previousPeriodEnd;
        Instant periodEnd = calculateSubscriptionPeriodEnd(requestedPeriodEnd, periodStart,
                subscription.getBillingCycle());

        //! step 9 - close the period that just ended. Its dates are left exactly as they are:
        //! it ran its full course, which is the difference between this and #16, where the old
        //! row's end is trimmed to the day the school left the plan.
        String previousSubscriptionNo = subscription.getSubscriptionNo();

        subscription.setCurrent(false);
        subscription.setReasonForChanges("Renewed into the next billing period. This row is the "
                + "period ending " + previousPeriodEnd + ".");

        //! step 10 - written BEFORE the new row, because the unique partial index on
        //! {schoolId, current} allows one current row per school and the old one still claims it
        // TODO: update current school subscription
        schoolSubscription.save(subscription);

        //! step 11 - a number of its own, for the same reason #16 allocates one: two rows of one
        //! school cannot share a subscriptionNo, and an invoice pointing at a number matching two
        //! records would be unanswerable.
        String subscriptionNumber = numberSequences.next(schoolId, NumberSequenceType.SUBSCRIPTION,
                "SUB/{YYYY}/{MM}/");

        //! step 12 - the next period, on identical terms. Everything negotiable is copied rather
        //! than re-derived from the plan: a school renewing keeps the price and the ceilings it
        //! actually had, including negotiated ones, because nobody agreed to renegotiate them by
        //! renewing. That is the opposite of #16, where a different plan means different terms.
        SchoolSubscription renewed = SchoolSubscription.builder()
                .schoolId(schoolId)
                .subscriptionNo(subscriptionNumber)
                .planDefinitionDocsId(subscription.getPlanDefinitionDocsId())
                .planVersion(subscription.getPlanVersion())
                // ACTIVE whichever of the three it was. An EXPIRED row whose new period has just
                // started would contradict its own dates, and a PAST_DUE one carried across would
                // say the new period is already unpaid before anything has been invoiced for it.
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(subscription.getBillingCycle())
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .autoRenew(subscription.getAutoRenew())
                .contractedPrice(subscription.getContractedPrice())
                .currencyCode(subscription.getCurrencyCode())
                .maxStudentsOverride(subscription.getMaxStudentsOverride())
                .maxUsersOverride(subscription.getMaxUsersOverride())
                .billingCustomerReference(subscription.getBillingCustomerReference())
                .reasonForChanges("Renewed from " + previousSubscriptionNo + " on the same terms.")
                .current(true)
                .build();

        // TODO: insert school subscription
        SchoolSubscription saved = schoolSubscription.save(renewed);

        //! step 13 - one history row, against the row the school moved onto. Both plan ids are
        //! the same plan, and that is worth writing down rather than leaving null: it is what
        //! distinguishes a renewal from a plan change in a list of history rows.
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(SubscriptionEventType.RENEWED)
                .previousStatus(statusBefore)
                .newStatus(saved.getStatus())
                .previousPlanDefinitionDocsId(subscription.getPlanDefinitionDocsId())
                .newPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason("Renewed on the same terms: '" + plan.getPlanCode() + "' version "
                        + plan.getPlanVersion() + ". " + previousSubscriptionNo + " covered the "
                        + "period ending " + previousPeriodEnd + " and was closed; "
                        + saved.getSubscriptionNo() + " covers " + periodStart + " to "
                        + periodEnd + ".")
                .performedByDocsId(null)
                .effectiveAt(periodStart)
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 14 - the note. What the renewal did, what it did NOT do about the money, and
        //! anything standing about the subscription a reader needs either way.
        List<String> note = new ArrayList<>();
        note.add("Renewed on the same terms: '" + plan.getPlanCode() + "' version "
                + plan.getPlanVersion() + " at " + saved.getContractedPrice() + " "
                + saved.getCurrencyCode() + ". " + previousSubscriptionNo + " is closed and kept "
                + "as history; this school is now on " + saved.getSubscriptionNo()
                + ", running from " + periodStart + " to " + periodEnd + " on its "
                + saved.getBillingCycle() + " cycle.");

        note.add("NO invoice was raised and no money was taken: nothing writes "
                + "subscription_invoices yet, so this moved the billing period and recorded the "
                + "renewal without charging for it.");

        if (statusBefore == SubscriptionStatus.EXPIRED) {
            note.add("It was EXPIRED and is now ACTIVE — this renewal is what a lapsed "
                    + "subscription was waiting for.");
        }
        if (statusBefore == SubscriptionStatus.PAST_DUE) {
            note.add("It was PAST_DUE and is now ACTIVE. Whatever was outstanding on the previous "
                    + "period is NOT settled by this — nothing here takes a payment.");
        }

        String standing = describeSubscriptionState(saved, plan);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 19 — cut a school off for non-payment ---------------------------------

    /**
     * #19 — suspends a subscription, and the school with it.
     *
     * <p><b>Why it is not #14 writing a status.</b> #14 can put {@code SUSPENDED} in the status
     * field, and that is the problem: cutting a school off stops its staff working, and it should
     * not be reachable by the same request that pushes a date out. This knows one transition,
     * refuses everything else, and carries the school's own access with it.
     *
     * <p><b>Two documents move, because one would stop nothing.</b> The subscription goes
     * {@code SUSPENDED}, which turns every feature off through #34's {@code allowed}; the school
     * goes {@code SUSPENDED} too, which is what {@code CurrentSchoolResolver.requireUsable()}
     * reads. Writing only the subscription would leave a "suspended" school still editing its own
     * records.
     *
     * <p><b>ACTIVE and PAST_DUE only.</b> A trial has no unpaid bill behind it, so cutting one off
     * is a different decision and is refused — which is also what lets #20 resume to
     * {@code ACTIVE} without looking anything up. {@code CANCELLED} and {@code EXPIRED} ended
     * rather than paused, and one already {@code SUSPENDED} has nothing to do.
     *
     * <p><b>It stamps no date on the subscription.</b> When it happened is the {@code effectiveAt}
     * of the {@code SUSPENDED} history row this writes; a second copy on the document could only
     * ever disagree with it. The school does get {@code suspendedAt}, because that field already
     * exists and core's own suspend maintains it.
     *
     * <p><b>What it does not do:</b> nothing kills the school's live sessions or halts its
     * scheduled jobs — neither exists yet — so a user already signed in is refused at the next
     * request that checks rather than thrown out. The response says so.
     */
    @Transactional
    public SubscriptionDetailResponse suspendSubscription(String schoolId, String subscriptionNo,
            SubscriptionSuspendRequest request) {

        //! step 1 - the school has to exist, and be one there is any point cutting off. The same
        //! allow-list #16 and #17 use: a school already closing is not suspended, it is going.
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        boolean schoolIsStillRunning = school.getStatus() == SchoolStatus.PROVISIONING
                || school.getStatus() == SchoolStatus.ACTIVE
                || school.getStatus() == SchoolStatus.SUSPENDED;

        if (!schoolIsStillRunning) {
            throw ApiException.conflict("SCHOOL_NOT_SUSPENDABLE",
                    "'" + school.getSchoolName() + "' is " + school.getStatus() + ", so there is "
                            + "nothing to cut off. Only a PROVISIONING, ACTIVE or SUSPENDED "
                            + "school can be suspended.");
        }

        //! step 2 - the subscription named in the URL, or the one they are on now
        SchoolSubscription subscription = findSchoolSubscription(school, schoolId, subscriptionNo);

        //! step 3 - one transition, and this is it. A trial is refused rather than suspended:
        //! there is no unpaid bill behind a trial, so this is not the decision being made — and
        //! refusing it here is what lets #20 resume to ACTIVE without asking what it was before.
        SubscriptionStatus previousStatus = subscription.getStatus();

        if (previousStatus != SubscriptionStatus.ACTIVE
                && previousStatus != SubscriptionStatus.PAST_DUE) {

            String because = switch (previousStatus) {
                case SUSPENDED -> "It is already suspended.";
                case TRIAL -> "A trial has no unpaid bill behind it. Ending a trial early is a "
                        + "status edit, or a plan change if the school is buying something.";
                case CANCELLED, EXPIRED -> "It has ended rather than paused, so there is no "
                        + "access left to stop.";
                default -> "";
            };

            throw ApiException.conflict("SUBSCRIPTION_NOT_SUSPENDABLE",
                    subscription.getSubscriptionNo() + " is " + previousStatus + ", so it cannot "
                            + "be suspended. Only ACTIVE and PAST_DUE can. " + because);
        }

        //! step 4 - the plan behind it, for the response only. Read before anything moves so the
        //! answer names the plan the school is locked out of.
        // TODO: read plan
        PlanDefinition plan = loadPlanBehindSubscription(subscription);

        //! step 5 - stop the subscription. No date field: the history row's effectiveAt is when
        //! it happened, and a second copy here could only ever disagree with it.
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        subscription.setReasonForChanges(request.reason().trim());

        // TODO: update school subscription
        SchoolSubscription saved = schoolSubscription.save(subscription);

        //! step 6 - stop the school, which is the half that actually blocks anything. Only an
        //! ACTIVE school moves: a PROVISIONING one was never usable, and one already SUSPENDED
        //! keeps the suspendedAt it has rather than having the clock reset by a second reason.
        String schoolNote;
        if (school.getStatus() == SchoolStatus.ACTIVE) {
            school.setStatus(SchoolStatus.SUSPENDED);
            school.setSuspendedAt(Instant.now());
            school.setStatusReason(request.reason().trim());

            // TODO: update school
            schools.save(school);
            schoolNote = "The school is now SUSPENDED too, which is what actually blocks it — "
                    + "the subscription's status turns features off, the school's status turns "
                    + "the tenant off.";
        } else if (school.getStatus() == SchoolStatus.SUSPENDED) {
            schoolNote = "The school was already SUSPENDED, so its suspendedAt and reason are "
                    + "left as they were.";
        } else {
            schoolNote = "The school is " + school.getStatus() + " and was left alone — it is "
                    + "not usable yet, so there is nothing to block.";
        }

        //! step 7 - one history row, saying what it was and what it became
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(SubscriptionEventType.SUSPENDED)
                .previousStatus(previousStatus)
                .newStatus(saved.getStatus())
                .previousPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .newPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason("Suspended from " + previousStatus + " for non-payment. "
                        + request.reason().trim())
                .performedByDocsId(null)
                .effectiveAt(Instant.now())
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 8 - the note. What stopped, what did NOT stop, and anything standing.
        List<String> note = new ArrayList<>();
        note.add("Suspended from " + previousStatus + ". Every feature is now refused: #34 reads "
                + "SUSPENDED and answers allowed:false on all of them.");
        note.add(schoolNote);
        note.add("NOTHING killed the school's live sessions or stopped its scheduled jobs — "
                + "neither exists yet — so a user already signed in is refused at the next "
                + "request that checks rather than thrown out now.");
        note.add("The period was not paused: it still ends " + saved.getCurrentPeriodEnd()
                + ", so the school is losing time it has paid for. Crediting that is a money "
                + "decision nothing here can make.");

        String standing = describeSubscriptionState(saved, plan);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 20 — switch a school back on after it pays ----------------------------

    /**
     * #20 — resumes a suspended subscription, and the school with it.
     *
     * <p>The exact reverse of #19 and only that: the subscription returns to {@code ACTIVE} and
     * the school with it. The plan, the price, the ceilings and the period are all untouched — a
     * suspension pauses access, and lifting it renegotiates nothing.
     *
     * <p><b>It resumes to ACTIVE without looking anything up.</b> #19 only ever suspends an
     * {@code ACTIVE} or a {@code PAST_DUE} subscription, and a school that has paid is not
     * {@code PAST_DUE} any more — so there is no case where the right answer is anything else.
     * Refusing to suspend a trial is what buys that simplicity.
     *
     * <p><b>The period is not extended</b>, and that is deliberate. A school suspended for three
     * weeks comes back to the same {@code currentPeriodEnd}, having paid for time it could not
     * use. Nothing here raises or credits an invoice, so moving the date would be this endpoint
     * inventing a refund; the response says so instead. If a credit was agreed, #14 is where the
     * date moves.
     *
     * <p><b>SUSPENDED only.</b> An {@code ACTIVE} subscription has nothing to resume, and a
     * {@code CANCELLED} or {@code EXPIRED} one ended rather than paused — bringing that back
     * would be selling a period without saying so, which is #13 or #16.
     */
    @Transactional
    public SubscriptionDetailResponse resumeSubscription(String schoolId, String subscriptionNo,
            SubscriptionResumeRequest request) {

        //! step 1 - the school has to exist, and be one worth switching back on. Resuming a
        //! school that is being wound down would reverse the wind-down as a side effect, the
        //! same reason #16 refuses those four states.
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        boolean schoolIsStillRunning = school.getStatus() == SchoolStatus.PROVISIONING
                || school.getStatus() == SchoolStatus.ACTIVE
                || school.getStatus() == SchoolStatus.SUSPENDED;

        if (!schoolIsStillRunning) {
            throw ApiException.conflict("SCHOOL_NOT_RESUMABLE",
                    "'" + school.getSchoolName() + "' is " + school.getStatus() + ", so it "
                            + "cannot be switched back on. Only a PROVISIONING, ACTIVE or "
                            + "SUSPENDED school can be resumed.");
        }

        //! step 2 - the subscription named in the URL, or the one they are on now
        SchoolSubscription subscription = findSchoolSubscription(school, schoolId, subscriptionNo);

        //! step 3 - one transition, and only from SUSPENDED. A finished subscription is not
        //! resumed: it ended rather than paused, and reopening it would be selling a period.
        SubscriptionStatus previousStatus = subscription.getStatus();

        if (previousStatus != SubscriptionStatus.SUSPENDED) {
            String because = previousStatus == SubscriptionStatus.CANCELLED
                    || previousStatus == SubscriptionStatus.EXPIRED
                    ? " It ended rather than paused — reopening it would be selling a period, "
                            + "which is a new subscription or a plan change."
                    : " There is nothing to resume.";

            throw ApiException.conflict("SUBSCRIPTION_NOT_RESUMABLE",
                    subscription.getSubscriptionNo() + " is " + previousStatus + ", so it cannot "
                            + "be resumed. Only a SUSPENDED subscription can be." + because);
        }

        //! step 4 - the plan behind it, for the response only
        // TODO: read plan
        PlanDefinition plan = loadPlanBehindSubscription(subscription);

        //! step 5 - switch the subscription back on. Nothing else moves: the plan, the price,
        //! the ceilings and both period dates are exactly as they were.
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setReasonForChanges(request.reason().trim());

        // TODO: update school subscription
        SchoolSubscription saved = schoolSubscription.save(subscription);

        //! step 6 - switch the school back on, which is the half that restores access.
        //! suspendedAt is deliberately left standing: it is when the suspension started, and a
        //! resumed school's history is worth keeping. Core's own reactivate leaves it too.
        String schoolNote;
        if (school.getStatus() == SchoolStatus.SUSPENDED) {
            school.setStatus(SchoolStatus.ACTIVE);
            school.setStatusReason(request.reason().trim());

            // TODO: update school
            schools.save(school);
            schoolNote = "The school is ACTIVE again, so the tenant is reachable. Its "
                    + "suspendedAt is left standing — that is when the suspension began, and it "
                    + "is worth keeping.";
        } else {
            schoolNote = "The school was " + school.getStatus() + " rather than SUSPENDED, so "
                    + "only the subscription moved.";
        }

        //! step 7 - one history row
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(SubscriptionEventType.RESUMED)
                .previousStatus(previousStatus)
                .newStatus(saved.getStatus())
                .previousPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .newPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason("Resumed after payment. " + request.reason().trim())
                .performedByDocsId(null)
                .effectiveAt(Instant.now())
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 8 - the note. What came back, and the one thing that did not.
        List<String> note = new ArrayList<>();
        note.add("Resumed to ACTIVE. Every feature the plan includes is allowed again.");
        note.add(schoolNote);
        note.add("The period was NOT extended: it still ends " + saved.getCurrentPeriodEnd()
                + ", so the school has paid for the time it was locked out of. Crediting that is "
                + "a money decision nothing here can make — moving the date, if that is what was "
                + "agreed, is the edit endpoint.");

        String standing = describeSubscriptionState(saved, plan);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 21 — end the subscription ---------------------------------------------

    /**
     * #21 — ends a subscription, at the end of the paid period or straight away.
     *
     * <p><b>The school usually keeps working until the period it already paid for runs out.</b>
     * That is the default: a school cancelling mid-month has bought that month, and cutting it off
     * the same afternoon would be keeping its money and taking the product away.
     *
     * <p><b>How that is said with the fields that already exist.</b> The status goes
     * {@code CANCELLED} either way — the contract is over, and that is simply true. What decides
     * whether the school can still work is the <b>period</b>, because
     * {@code SchoolSubscriptionService.whyNotActive} now lets a cancelled subscription grant until
     * its {@code currentPeriodEnd} passes:
     *
     * <pre>
     * immediate absent or false  -> period left alone; access runs to currentPeriodEnd
     * immediate true             -> currentPeriodEnd trimmed to now; access stops at once
     * </pre>
     *
     * <p>So no flag says "cancelled but still running". The status says cancelled and the dates
     * say how long for, which is the same division of labour #16 uses when it closes a row.
     *
     * <p><b>The cancellation sticks without any new check.</b> #17 already refuses to renew a
     * {@code CANCELLED} subscription and #20 already refuses to resume one, so there is no path
     * that quietly undoes this. Bringing the school back means selling it something new — #13 or
     * #16.
     *
     * <p><b>The immediate shape trims the period, and the history row keeps what it was.</b>
     * Moving {@code currentPeriodEnd} to now is the same thing #16 does to the row a school
     * leaves: it records the period actually served. What that loses from the document — the end
     * date originally paid for — goes into the history row's reason, which is where #16 puts the
     * superseded number for the same reason.
     *
     * <p><b>The honest gap.</b> Nothing marks a lapsed subscription {@code EXPIRED}, so a
     * scheduled cancellation reads {@code CANCELLED} with {@code periodEnded: true} after its
     * date rather than {@code EXPIRED} — correct in every field, and still not tidied away.
     * <b>A job will close these</b>; #22 was dropped on 2026-09-08 because expiring is a date
     * arriving rather than a decision, the same conclusion #18 reached about {@code PAST_DUE}.
     *
     * <p><b>It does not touch the school.</b> Unlike #19, which takes the school's access down
     * with it, this is a commercial end and not a lock-out. Winding the tenant down is core's
     * business, and doing it here would make one request mean two decisions.
     *
     * <p><b>No money moves.</b> An immediate cancellation keeps whatever was paid for the part of
     * the period being given up, because nothing here raises, credits or refunds an invoice.
     *
     * <p><b>Almost every status can be cancelled</b>, the opposite of #19 and #20: a trial that
     * did not convert, a suspended school that never paid, one that is {@code PAST_DUE}. Only a
     * subscription that is genuinely finished is refused.
     */
    @Transactional
    public SubscriptionDetailResponse cancelSubscription(String schoolId, String subscriptionNo,
            SubscriptionCancelRequest request) {

        //! step 1 - the school has to exist. A CLOSED or OFFBOARDING one is deliberately ALLOWED
        //! here, unlike on #19 and #20: cancelling the subscription is part of winding a school
        //! down, and refusing it would leave a closed school with a live subscription nobody
        //! could end. Only a deleted school is refused — there is nothing left to be right about.
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        if (school.getStatus() == SchoolStatus.DELETED
                || school.getStatus() == SchoolStatus.DELETION_PENDING) {
            throw ApiException.conflict("SUBSCRIPTION_NOT_CANCELLABLE",
                    "'" + school.getSchoolName() + "' is " + school.getStatus() + ", so its "
                            + "subscription is past cancelling.");
        }

        //! step 2 - the subscription named in the URL, or the one they are on now
        SchoolSubscription subscription = findSchoolSubscription(school, schoolId, subscriptionNo);

        //! step 3 - what is genuinely finished cannot be ended again. EXPIRED is over. CANCELLED
        //! is over ONLY once its period has run out — before that it is a cancellation still
        //! serving out its time, and escalating it to immediate is a real decision rather than a
        //! repeat, so that one is allowed through.
        SubscriptionStatus previousStatus = subscription.getStatus();
        Instant paidUntil = subscription.getCurrentPeriodEnd();
        boolean periodStillRunning = paidUntil != null && paidUntil.isAfter(Instant.now());

        if (previousStatus == SubscriptionStatus.EXPIRED) {
            throw ApiException.conflict("SUBSCRIPTION_ALREADY_ENDED",
                    subscription.getSubscriptionNo() + " is EXPIRED, so there is nothing left "
                            + "to end.");
        }

        if (previousStatus == SubscriptionStatus.CANCELLED) {
            if (!periodStillRunning) {
                throw ApiException.conflict("SUBSCRIPTION_ALREADY_ENDED",
                        subscription.getSubscriptionNo() + " was cancelled and its period ended "
                                + "on " + paidUntil + ", so there is nothing left to end.");
            }
            if (!request.isImmediate()) {
                throw ApiException.conflict("CANCELLATION_ALREADY_SCHEDULED",
                        subscription.getSubscriptionNo() + " is already cancelled and runs out "
                                + "on " + paidUntil + ". Send immediate: true to stop its access "
                                + "now instead.");
            }
        }

        //! step 4 - the plan behind it, for the response only
        // TODO: read plan
        PlanDefinition plan = loadPlanBehindSubscription(subscription);

        //! step 5 - end it. The status is the same either way, because the contract is over
        //! either way; what differs is how long the access lasts, and that is the period's job.
        //!
        //! autoRenew goes false as well. It enforces nothing on its own — #17 refuses a
        //! CANCELLED subscription outright — but leaving it true would have the school's own
        //! billing screen say its cancelled subscription renews automatically.
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(Boolean.FALSE);
        subscription.setReasonForChanges(request.reason().trim());

        if (request.isImmediate()) {
            //! Trimmed to now, which is what stops the access: whyNotActive refuses a cancelled
            //! subscription whose period has run out. The same thing #16 does to a row a school
            //! leaves — it records the period actually served.
            subscription.setCurrentPeriodEnd(Instant.now());
        }

        // TODO: update school subscription
        SchoolSubscription saved = schoolSubscription.save(subscription);

        //! step 6 - one history row. effectiveAt is when the cancellation takes effect: now for
        //! an immediate one, the period end for a scheduled one — the only place in this service
        //! where that field is deliberately in the future, and the only honest value for it.
        //!
        //! The reason carries the end date originally paid for, because an immediate cancellation
        //! has just overwritten it on the document and this is the only place it survives.
        SubscriptionHistory historyEntry = SubscriptionHistory.builder()
                .schoolId(schoolId)
                .schoolSubscriptionDocsId(saved.getId())
                .eventType(SubscriptionEventType.CANCELLED)
                .previousStatus(previousStatus)
                .newStatus(saved.getStatus())
                .previousPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .newPlanDefinitionDocsId(saved.getPlanDefinitionDocsId())
                .source(SOURCE_ADMIN_PORTAL)
                .reason((request.isImmediate()
                        ? "Cancelled immediately from " + previousStatus + ". The period paid "
                                + "for ran to " + paidUntil + " and was trimmed to the "
                                + "cancellation. "
                        : "Cancelled from " + previousStatus + " with effect from " + paidUntil
                                + "; the school keeps working until then. ")
                        + request.reason().trim())
                .performedByDocsId(null)
                .effectiveAt(request.isImmediate() ? saved.getCurrentPeriodEnd() : paidUntil)
                .build();

        // TODO: insert history
        history.save(historyEntry);

        //! step 7 - the note. Which shape it took, what is still running, and what nothing does.
        List<String> note = new ArrayList<>();

        if (request.isImmediate()) {
            note.add("Cancelled immediately, from " + previousStatus + ". The period was trimmed "
                    + "from " + paidUntil + " to now, which is what stops the access: every "
                    + "feature is refused because the subscription is cancelled AND its period "
                    + "is over.");
            note.add("NO money was refunded for the rest of that period. Nothing here raises or "
                    + "credits an invoice, so what should happen to it is still an open "
                    + "question. The history row keeps the date originally paid for.");
        } else {
            note.add("Cancelled, and the school keeps working until " + paidUntil + " — the "
                    + "period it has already paid for. The status says CANCELLED because the "
                    + "contract is over; the period says how long the access lasts.");
            note.add("It will NOT be renewed or resumed: #17 refuses a cancelled subscription "
                    + "and so does #20, so nothing quietly undoes this. Bringing the school back "
                    + "means selling it a new subscription or changing its plan.");
            note.add("NOTHING marks it EXPIRED when that date passes. It will read CANCELLED "
                    + "with periodEnded true — every field correct, and still not tidied away. "
                    + "A job will close these; there is no endpoint for it, because a period "
                    + "end passing is a date arriving rather than a decision.");
        }

        note.add("The school itself is untouched at " + school.getStatus() + ": this is a "
                + "commercial end, not a lock-out. Winding the tenant down is core's business.");

        String standing = describeSubscriptionState(saved, plan);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 27 — what one school is on right now ----------------------------------

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
     * <p><b>It reports a lapsed period rather than hiding it.</b> Nothing marks a subscription
     * expired on its own — #21 and #26 are not built — and #17 starts the next period only when
     * somebody calls it, which nothing does on a schedule yet. So a period can run out while the
     * status still says the school is paying. The response says so in {@code periodEnded} and in
     * {@code note}, because a screen trusting {@code status} alone would show a school as live
     * months after its period ended.
     */
    public SubscriptionDetailResponse getSubscription(String schoolId) {

        //! step 1 - the school's current subscription
        // TODO: read subscription
        SchoolSubscription subscription = schoolSubscription
                .findBySchoolIdAndCurrentIsTrue(schoolId)
                .orElseGet(() -> {
                        School school = schools.findById(schoolId).orElse(null);

                        if (school == null) {
                        throw ApiException.notFound("SCHOOL_NOT_FOUND",
                                "No school found with id '" + schoolId + "'.");
                        }

                        throw ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                                "'" + school.getSchoolName() + "' has no subscription. Create one first.");
                });

        //! step 2 - the plan it points at, for the name, limits and features
        PlanDefinition plan = loadPlanBehindSubscription(subscription);

        return SubscriptionDetailResponse.fromSubscription(
                subscription,
                plan,
                describeSubscriptionState(subscription, plan));
        }
    
    

    //! Endpoint 33 — the school's own billing screen ----------------------------------
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

    /*
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------
    */

    /** The plan version a school can be assigned to today; drafts and retired plans are rejected, while private quotes remain valid.      *
     * Used by:
     * - createSubscription()
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

    /** Derives the first billing period end date from the plan cycle using fixed-day periods; CUSTOM requires the caller to specify it.      *
     * Used by:
     * - createSubscription()
     */
    private Instant calculateSubscriptionPeriodEnd(Instant requested, Instant periodStart, BillingCycle cycle) {

        if (requested != null) {
            if (!requested.isAfter(periodStart)) {
                throw ApiException.badRequest("INVALID_BILLING_PERIOD",
                        "currentPeriodEnd (" + requested + ") must be after currentPeriodStart ("
                                + periodStart + ").");
            }
            return requested;
        }

        // Days, so no calendar and no zone is needed: an Instant can add days on its own, where
        // Instant.plus(1, MONTHS) throws because a month is not a fixed number of seconds.
        long days = switch (cycle) {
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case HALF_YEARLY -> 180;
            case YEARLY -> 365;
            case CUSTOM -> throw ApiException.badRequest("BILLING_PERIOD_END_REQUIRED",
                    "This plan bills on a CUSTOM cycle, which has no set length, so "
                            + "currentPeriodEnd has to be sent.");
        };

        return periodStart.plus(days, ChronoUnit.DAYS);
    }

            /** Returns today's start in the school's timezone, falling back to UTC if unavailable.      *
     * Used by:
     * - createSubscription()
     */
        private Instant startOfTodayInSchoolZone(String schoolTimeZone) {
        ZoneId zone;
        try {
                zone = (schoolTimeZone == null || schoolTimeZone.isBlank())
                        ? ZoneOffset.UTC
                        : ZoneId.of(schoolTimeZone.trim());
        } catch (DateTimeException e) {
                zone = ZoneOffset.UTC;
        }

        return LocalDate.now(zone).atStartOfDay(zone).toInstant();
        }

    /**
     * Anything about this subscription worth saying out loud.
     *
     * <p>Each of these is a state the module can genuinely be in today, and each one would
     * otherwise be read wrongly off a single field.
     *
     * Used by:
     * - updateSubscription()
     * - getSubscription()
     */
    private String describeSubscriptionState(SchoolSubscription subscription, PlanDefinition plan) {
        List<String> notes = new ArrayList<>();

        Instant end = subscription.getCurrentPeriodEnd();
        boolean live = subscription.getStatus() == SubscriptionStatus.ACTIVE
                || subscription.getStatus() == SubscriptionStatus.TRIAL;

        if (end != null && !end.isAfter(Instant.now()) && live) {
            notes.add("The period ended on " + end + " but the status still says "
                    + subscription.getStatus() + ". Nothing marks a subscription expired on its "
                    + "own yet, so this has to be read as lapsed rather than paying — renewing "
                    + "it starts the next period.");
        }

        if (subscription.getStatus() == SubscriptionStatus.TRIAL) {
            notes.add("This is a trial. It becomes a paying subscription either by setting its "
                    + "status through #14, or — when the school is buying a different plan from "
                    + "the one it tried — by changing its plan. Renewing it is refused: nobody "
                    + "has agreed what the next period costs.");
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
          *
     * Used by:
     * - updateSubscription()
     */
    private SchoolSubscription findSchoolSubscription(School school, String schoolId,
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
     * Writes the request onto the subscription, and returns the fields that actually moved.
     *
     * <p><b>"Changed" means different from what was stored.</b> A caller who resends the current
     * cycle has not edited anything, and recording that they did would fill the audit trail with
     * rows that explain nothing. So every field is compared before it is set.
     *
     * <p>{@code Objects.equals} throughout, because every field here is nullable — an override
     * that is not set is null, and null has to compare equal to itself.
     *
     * <p><b>{@code reasonForChanges} is not in the list.</b> It is written by the caller on every
     * edit, so it always "changed" — reporting it would put "reasonForChanges" in every history
     * row's field list and in every response note, next to the reason itself.
          *
     * Used by:
     * - updateSubscription()
     */
    private List<String> applySubscriptionEdits(SchoolSubscription subscription,
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

        //! the two capacity ceilings. ZERO MEANS "TAKE IT AWAY": the fields are flat, and
        //! Jackson hands over null both for a field that was omitted and for one sent as null,
        //! so "leave this alone" and "remove this" would arrive identical. Zero can carry the
        //! removal because it cannot mean anything else — nobody negotiates a ceiling of no
        //! students. A negative number is a typo, not an instruction.
        if (request.maxStudentsOverride() != null) {
            if (request.maxStudentsOverride() < 0) {
                throw ApiException.badRequest("LIMIT_TOO_LOW",
                        "maxStudentsOverride cannot be negative. Received: "
                                + request.maxStudentsOverride() + ". Send 0 to remove the "
                                + "override and use the plan's own limit, or omit it to leave "
                                + "the override as it is.");
            }
            Long ceiling = request.maxStudentsOverride() == 0
                    ? null
                    : request.maxStudentsOverride();

            if (!Objects.equals(ceiling, subscription.getMaxStudentsOverride())) {
                subscription.setMaxStudentsOverride(ceiling);
                changed.add("maxStudentsOverride");
            }
        }

        if (request.maxUsersOverride() != null) {
            if (request.maxUsersOverride() < 0) {
                throw ApiException.badRequest("LIMIT_TOO_LOW",
                        "maxUsersOverride cannot be negative. Received: "
                                + request.maxUsersOverride() + ". Send 0 to remove the override "
                                + "and use the plan's own limit, or omit it to leave the "
                                + "override as it is.");
            }
            Long ceiling = request.maxUsersOverride() == 0
                    ? null
                    : request.maxUsersOverride();

            if (!Objects.equals(ceiling, subscription.getMaxUsersOverride())) {
                subscription.setMaxUsersOverride(ceiling);
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
          *
     * Used by:
     * - updateSubscription()
     */
    private SubscriptionEventType chooseHistoryEventType(SubscriptionStatus previousStatus,
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
     * <p>The field list and the caller's own words, together. Months later "the dates changed" is
     * the question and "which fields moved" is the answer — a reason of "renegotiated" alone does
     * not say what was renegotiated, and a field list alone does not say why.
     *
     * <p>The blank branch is kept although {@code @NotBlank} makes it unreachable from #14: the
     * lifecycle endpoints will share this method, and not all of them will take a reason.
          *
     * Used by:
     * - updateSubscription()
     */
    private String buildHistoryReason(List<String> changed, String callerReason) {
        String fields = "Edited " + String.join(", ", changed) + ".";

        return callerReason == null || callerReason.isBlank()
                ? fields
                : fields + " " + callerReason.trim();
    }

    /** The plan a subscription points at, which must exist for the response to be complete.      *
     * Used by:
     * - updateSubscription()
     * - getSubscription()
     */
    private PlanDefinition loadPlanBehindSubscription(SchoolSubscription subscription) {
        // TODO: read plan
        return planDefinition.findById(subscription.getPlanDefinitionDocsId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND",
                        "The plan " + subscription.getSubscriptionNo() + " points at no longer "
                                + "exists."));
    }

    /**
     * What the move did, and what it deliberately did not do to the money.
     *
     * <p>Three things a caller needs and cannot read off the response's fields:
     *
     * <ul>
     * <li><b>Which plan it came from, and which row.</b> The response is the NEW row, so
     * without this the reader cannot tell an upgrade from a downgrade, nor find the closed row
     * the school was on before.</li>
     * <li><b>That no money moved.</b> The school is part-way through a period it paid for, and
     * this endpoint charges, credits and refunds nothing — because nothing in this codebase
     * raises an invoice. Saying so is the difference between a plan moved and a payment somebody
     * thinks was taken.</li>
     * <li><b>That a downgrade was not checked.</b> Nothing counts students yet, so moving a
     * school to a smaller plan may leave it above its new ceiling and nobody would know.</li>
     * <li><b>Whether the ceilings were negotiated or inherited.</b> {@code maxStudents} on the
     * response is the figure in force either way, so only the note can say which.</li>
     * </ul>
     *
     * <p>It says nothing about the subscription's standing state — a lapsed period, a trial, a
     * retired plan. That is {@link #describeSubscriptionState}, and {@code changePlan} joins the
     * two: one note helper calling the other would bury half the sentence.
     *
     * Used by:
     * - changePlan()
     */
    private String describePlanMove(PlanDefinition previousPlan, PlanDefinition newPlan,
            SchoolSubscription saved, String previousSubscriptionNo) {

        String direction = newPlan.getListPrice().compareTo(previousPlan.getListPrice()) > 0
                ? "Upgraded"
                : newPlan.getListPrice().compareTo(previousPlan.getListPrice()) < 0
                        ? "Downgraded"
                        : "Moved";

        String money = "NO money has moved for the period the school had already paid for: "
                + "nothing raises invoices yet, so nothing was charged, credited or refunded. "
                + "What should happen to it is still an open question.";

        // Only when the ceilings are not the new plan's own, which means this request named
        // them: worth saying, because maxStudents on the response is the figure in force either
        // way and does not reveal whether it was negotiated or inherited.
        String ceiling = Objects.equals(saved.getMaxStudentsOverride(), newPlan.getMaxStudents())
                && Objects.equals(saved.getMaxUsersOverride(), newPlan.getMaxUsers())
                        ? ""
                        : " The ceilings are negotiated rather than the new plan's own: "
                                + saved.getMaxStudentsOverride() + " students and "
                                + saved.getMaxUsersOverride() + " users, against the plan's "
                                + newPlan.getMaxStudents() + " and " + newPlan.getMaxUsers()
                                + ".";

        String downgrade = "Downgraded".equals(direction)
                ? " Nothing checks whether the school is already above the new plan's limits — "
                        + "nothing counts students yet — so a downgrade is not verified as safe."
                : "";

        return direction + " from '" + previousPlan.getPlanCode() + "' version "
                + previousPlan.getPlanVersion() + " to '" + newPlan.getPlanCode() + "' version "
                + newPlan.getPlanVersion() + ". " + previousSubscriptionNo + " is closed and kept "
                + "as history; this school is now on " + saved.getSubscriptionNo()
                + ", running from today to " + saved.getCurrentPeriodEnd() + " on the new plan's "
                + saved.getBillingCycle() + " cycle. " + money + ceiling + downgrade;
    }

    /**
     * What this edit did, and anything it has left inconsistent.
     *
     * <p>A cycle that no longer matches the plan's is reported rather than corrected — billing a
     * school monthly on a plan that bills yearly is a real arrangement, and rewriting it would
     * undo a deliberate change — but left unsaid it would be found on an invoice instead.
     *
     * <p><b>It says nothing about the subscription's standing state</b> — a lapsed period, a
     * trial, a retired plan. That is {@link #describeSubscriptionState}, and
     * {@code updateSubscription} joins the two: a helper calling the other helper would bury half
     * the sentence somewhere a reader would not look for it.
     *
     * Used by:
     * - updateSubscription()
     */
    private String describeEditOutcome(SchoolSubscription subscription, PlanDefinition plan,
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

        return String.join(" ", notes);
    }

    /**
     * An override that lowers nothing and raises nothing is not an override.
     *
     * <p>Used where a ceiling is being <b>set</b> rather than edited — a sale (#13) and a plan
     * change (#16). Neither has an override to take away, so zero there is a mistake like any
     * other. #14 is the one place zero means "remove it", and it makes that check itself.
     *
     * Used by:
     * - createSubscription()
     * - changePlan()
     */
    private void validateCapacityOverrideIsAtLeastOne(String label, Long value) {
        if (value != null && value < 1) {
            throw ApiException.badRequest("LIMIT_TOO_LOW",
                    label + " must be at least 1 when it is sent. Received: " + value
                            + ". Omit it to use the plan's own limit.");
        }
    }

    /**
     * Refuses a billing period that starts before today.
     *
     * <p>A period start is a commitment about when billing begins, and one in the past says the
     * school has been paying for time nobody charged it for. There is nothing in the codebase
     * that could reconcile that — no invoice is raised for a period at all — so a backdated start
     * would sit in the record as a figure no process could act on.
     *
     * <p><b>Today counts.</b> The comparison is against the start of today <i>in the school's own
     * timezone</i>, matching the default this endpoint uses when no start is sent, so "today" is
     * the same day to the operator and to the school. A start at any point later today is fine —
     * only a day already finished is refused.
     *
     * <p><b>Null is fine, and means "today".</b> The field is optional on both requests: absent
     * on a sale takes today, and absent on an edit leaves the stored start alone.
     *
     * <p><b>It never looks at what is already stored.</b> A subscription sold months ago has a
     * start in the past by definition; checking that would make every other field on #14
     * unreachable for a running subscription. Only a value somebody is sending now is a decision
     * to refuse — which on #13 and #16 is every call, because both require the field.
     *
     * Used by:
     * - createSubscription()
     * - updateSubscription()
     * - changePlan()
     */
    private void validatePeriodStartIsTodayOrLater(Instant requestedStart, School school) {
        if (requestedStart == null) {
            return;
        }

        Instant startOfToday = startOfTodayInSchoolZone(school.getDefaultTimeZone());

        if (requestedStart.isBefore(startOfToday)) {
            throw ApiException.badRequest("PERIOD_START_IN_PAST",
                    "currentPeriodStart (" + requestedStart + ") is before the start of today in "
                            + "the school's timezone (" + startOfToday + "). A billing period "
                            + "starts today or later — nothing here can invoice a period that "
                            + "has already run.");
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
          *
     * Used by:
     * - createSubscription()
     */
    private String activateSchoolIfSetupComplete(School school) {
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

    /** What the caller should know next, including what just happened to the school.      *
     * Used by:
     * - createSubscription()
     */
    private String describeCreateOutcome(SchoolSubscription subscription, boolean trial, String activation) {
        String base = trial
                ? "Trial started, running to " + subscription.getCurrentPeriodEnd() + "."
                : "Subscribed, and billed from " + subscription.getCurrentPeriodStart() + ".";

        return base + activation + " No invoice has been raised: that is a separate step.";
    }
}
