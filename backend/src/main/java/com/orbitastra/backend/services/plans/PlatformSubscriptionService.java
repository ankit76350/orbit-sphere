package com.orbitastra.backend.services.plans;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionCreateRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionPlanChangeRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionRenewRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionResumeRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionSearchRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionSuspendRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionUpdateRequest;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionCancelRequest;
import com.orbitastra.backend.dto.plans.subscription.response.MySubscriptionResponse;
import com.orbitastra.backend.dto.plans.subscription.response.SubscriptionDetailResponse;
import com.orbitastra.backend.dto.plans.subscription.response.SubscriptionResponse;
import com.orbitastra.backend.dto.plans.subscription.response.SubscriptionSummaryResponse;
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
import com.orbitastra.backend.repositories.plans.plandefinition.PlanDefinitionRepository;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;
import com.orbitastra.backend.repositories.plans.subscriptionhistory.SubscriptionHistoryRepository;
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

    /** Allowed fields for sorting subscription history. */
    private static final Map<String, String> SORTABLE_SUBSCRIPTION_FIELDS = new LinkedHashMap<>();
    static {
        SORTABLE_SUBSCRIPTION_FIELDS.put("currentperiodstart", "currentPeriodStart");
        SORTABLE_SUBSCRIPTION_FIELDS.put("currentperiodend", "currentPeriodEnd");
        SORTABLE_SUBSCRIPTION_FIELDS.put("subscriptionno", "subscriptionNo");
        SORTABLE_SUBSCRIPTION_FIELDS.put("status", "status");
        SORTABLE_SUBSCRIPTION_FIELDS.put("createdat", "createdAt");
        SORTABLE_SUBSCRIPTION_FIELDS.put("updatedat", "updatedAt");
    }

     /** Allowed sort field names shown in validation errors. */
    private static final String SORTABLE_SUBSCRIPTION_FIELD_NAMES =
            "currentPeriodStart, currentPeriodEnd, subscriptionNo, status, createdAt, updatedAt";

    /** Default order for subscription history: newest period first, then subscription number. */
    private static final Sort SUBSCRIPTION_HISTORY_ORDER = Sort.by(
            Sort.Order.desc("currentPeriodStart"),
            Sort.Order.desc("subscriptionNo"));

    /** URL value used to refer to the school's current subscription. */
    private static final String CURRENT_SUBSCRIPTION = "current";



    private final SchoolRepository schools;
    private final PlanDefinitionRepository planDefinition;
    private final SchoolSubscriptionRepository schoolSubscription;
    private final SubscriptionHistoryRepository history;
    private final NumberSequenceService numberSequences;
    private final PlanValidator planValidator;
    private final SchoolPlatformService schoolPlatform;




    //! Endpoint 13 — a school's first subscription ------------------------------------

    /** #13 — Puts a school on a plan and creates its first subscription history record. */
    @Transactional
    public SubscriptionResponse createSubscription(String schoolId,
            SubscriptionCreateRequest request) {

        //! step 1 - the school has to exist and be a school we can still sell to
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        // Every date this method puts in a message or a note is rendered in the SCHOOL's
        // zone, not UTC: a period starting at midnight in Asia/Kolkata is stored as
        // 18:30Z the day before, so UTC would name the wrong calendar day.
        String zone = school.getDefaultTimeZone();
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
        //! is the ordinary sale; a request that names one is selling the same feature access on
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
        //! end date and says so, and a CUSTOM plan sold as MONTHLY derives one and REFUSES one —
        //! the rule follows what the school is actually being billed on, not what the plan is
        //! listed at. Only a CUSTOM cadence takes an end date; the other four are their length.
        Instant periodEnd = calculateSubscriptionPeriodEnd(request.currentPeriodEnd(),
                periodStart, billingCycle, zone);

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
                describeCreateOutcome(savedSubscription, trial, activation, zone));
    }



    //! Endpoint 14 — edit what a school is contracted to ------------------------------
   /** #14 — Updates subscription terms and records the change in history. */
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

        // Every date this method puts in a message or a note is rendered in the SCHOOL's
        // zone, not UTC: a period starting at midnight in Asia/Kolkata is stored as
        // 18:30Z the day before, so UTC would name the wrong calendar day.
        String zone = school.getDefaultTimeZone();
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
        SubscriptionStatus previousStatus = subscription.getStatus();

        //! step 4 - only a subscription that is still running its ordinary life may be edited.
        //! TRIAL and ACTIVE, and nothing else.
        //!
        //! This endpoint writes what it is told with no transition rules, which is exactly why it
        //! should not reach a subscription whose state was somebody's decision: undoing a
        //! suspension, a cancellation or a lapsed period by editing a field would bypass the
        //! endpoint that owns that transition, and the history row it writes would say "edited"
        //! where the real event was "resumed" or "revived".
        //!
        //! Every refused state has a way back, and the message names it — that is what makes
        //! this safe rather than a dead end.
        if (previousStatus != SubscriptionStatus.TRIAL
                && previousStatus != SubscriptionStatus.ACTIVE) {

            String wayBack = switch (previousStatus) {
                case SUSPENDED -> "Resume it to lift the suspension.";
                case PAST_DUE -> "Renew it once the bill is settled, or change its plan — both "
                        + "put it back to ACTIVE.";
                case CANCELLED, EXPIRED -> "Change its plan to bring the school back, which "
                        + "opens a new period at ACTIVE.";
                default -> "";
            };

            throw ApiException.conflict("SUBSCRIPTION_NOT_EDITABLE",
                    subscription.getSubscriptionNo() + " is " + previousStatus + ", so its terms "
                            + "cannot be edited. Only a TRIAL or ACTIVE subscription can be. "
                            + wayBack);
        }

        //! step 5 - a cadence being SET names the dates it needs with it. Sending a cycle
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

        //! step 6 - and on the four fixed cadences an end date is refused, because the cadence
        //! already decides it. This endpoint writes the fields it is given straight onto the
        //! document, so without this an edit could store MONTHLY beside a period running six
        //! months — the two would contradict each other and the document would be believed.
        //!
        //! Read against the cadence the subscription will be on AFTER this edit, not the one it
        //! is on now: the pair (CUSTOM -> MONTHLY, plus an end date) has to be refused as one
        //! request rather than accepted because CUSTOM was true when it arrived.
        //!
        //! Not gated on billingCycle being sent, so a bare currentPeriodEnd on a subscription
        //! already billing MONTHLY is refused too. That is the case the caller is most likely to
        //! try, and the message names what to send instead.
        BillingCycle cadenceAfterEdit = request.billingCycle() == null
                ? subscription.getBillingCycle()
                : request.billingCycle();

        // "If the user is sending currentPeriodEnd AND the billing cycle is NOT CUSTOM, reject the request."
        if (request.currentPeriodEnd() != null && cadenceAfterEdit != BillingCycle.CUSTOM) {
            throw ApiException.badRequest("BILLING_PERIOD_END_NOT_ALLOWED",
                    "currentPeriodEnd cannot be sent when the cadence is " + cadenceAfterEdit
                            + ", which decides its own period end. Send currentPeriodStart "
                            + "instead — the end moves with it — or send billingCycle to change "
                            + "the cadence. Only a CUSTOM cadence takes an end date.");
        }

        //! step 7 - a start being SET has to be today or later. The stored one is not checked:
        //! a subscription sold months ago has a start in the past by definition, and refusing to
        //! edit it would make every other field on this endpoint unreachable for a running
        //! subscription. Only a value on the request is a decision somebody is making now.
        validatePeriodStartIsTodayOrLater(request.currentPeriodStart(), school);

        //! step 8 - apply the edit, keeping a list of what actually moved. The list is what the
        //! history row and the response are built from, so "changed" means "different from what
        //! was stored", not "was mentioned in the request".
        List<String> changed = applySubscriptionEdits(subscription, request);

        if (changed.isEmpty()) {
            // TODO: read plan
            PlanDefinition unchangedPlan = loadPlanBehindSubscription(subscription);
            return SubscriptionDetailResponse.fromSubscription(subscription, unchangedPlan,
                    "Nothing changed: every field sent already held that value. No history row "
                            + "was written.");
        }

        //! step 9 - THE CADENCE DECIDES THE PERIOD, the same way it does on a sale. A cadence
        //! that moved leaves the stored end date describing a period nobody is on any more: an
        //! end derived as "start + 365" is not the end of a MONTHLY period, so keeping it would
        //! bill the school for a year while the document says it pays monthly.
        //!
        //! ON A FIXED CADENCE THE END IS ONLY EVER DERIVED, never sent — step 6 refuses an end
        //! date outright — so this is the only thing that writes it. Which makes the rule one
        //! sentence: the end moves when what it is measured from moves, and at no other time.
        //! An edit to the price or the capacity leaves the period exactly where it was.
        boolean cycleMoved = changed.contains("billingCycle");
        boolean startMoved = changed.contains("currentPeriodStart");

        //! A CUSTOM cadence keeps the end it was given, whether that arrived with this request or
        //! was already on record: that date was somebody's decision rather than arithmetic, so
        //! nothing here recomputes it, and a start that moves under it is checked against it by
        //! step 11 instead. So the only case left is a fixed cadence whose start or cycle moved.
        if (request.currentPeriodEnd() == null && (cycleMoved || startMoved)
                && subscription.getBillingCycle() != BillingCycle.CUSTOM) {

            Instant derivedEnd = calculateSubscriptionPeriodEnd(null,
                    subscription.getCurrentPeriodStart(), subscription.getBillingCycle(), zone);

            if (!derivedEnd.equals(subscription.getCurrentPeriodEnd())) {
                subscription.setCurrentPeriodEnd(derivedEnd);
                changed.add("currentPeriodEnd");
            }
        }

        //! step 10 - the reason goes onto the document as well as the history row. Written only
        //! now, because an edit that changed nothing has nothing to explain. No null check: the
        //! request has @NotBlank on it, so an unexplained edit never reaches here.
        subscription.setReasonForChanges(request.reason().trim());

        //! step 11 - the period has to still make sense after the edit, whichever end moved
        if (!subscription.getCurrentPeriodEnd().isAfter(subscription.getCurrentPeriodStart())) {
            throw ApiException.badRequest("INVALID_BILLING_PERIOD",
                    "currentPeriodEnd ("
                            + Dates.readable(subscription.getCurrentPeriodEnd(), zone)
                            + ") must be after currentPeriodStart ("
                            + Dates.readable(subscription.getCurrentPeriodStart(), zone)
                            + "). Editing one end of a period is checked against the other.");
        }

        // TODO: update school subscription
        SchoolSubscription saved = schoolSubscription.save(subscription);

        //! step 12 - one history row for the whole edit, in this same transaction
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

        //! step 13 - the plan is read only so the response can carry its features and limits
        // TODO: read plan
        PlanDefinition plan = loadPlanBehindSubscription(saved);

        //! step 14 - the note is two answers joined here rather than by one helper calling the
        //! other: what this edit did, and anything standing about the subscription that a
        //! reader needs whether or not it was edited.
        List<String> note = new ArrayList<>();
        note.add(describeEditOutcome(saved, plan, changed));

        String standing = describeSubscriptionState(saved, plan, zone);
        if (standing != null) {
            note.add(standing);
        }

        note.add("No invoice has been raised or credited: that is a separate step.");

        return SubscriptionDetailResponse.fromSubscription(saved, plan,
                String.join(" ", note));
    }


    //! Endpoint 16 — move a school onto a different plan ------------------------------

    /** #16 — Moves a school to a different plan or plan version. */
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

        // Every date this method puts in a message or a note is rendered in the SCHOOL's
        // zone, not UTC: a period starting at midnight in Asia/Kolkata is stored as
        // 18:30Z the day before, so UTC would name the wrong calendar day.
        String zone = school.getDefaultTimeZone();
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
        //! is actually billed on: a YEARLY plan billed CUSTOM needs an end date and says so,
        //! and a CUSTOM plan billed QUARTERLY refuses one because the cadence decides it.
        Instant periodEnd = calculateSubscriptionPeriodEnd(request.currentPeriodEnd(),
                periodStart, billingCycle, zone);

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
        note.add(describePlanMove(previousPlan, newPlan, saved, previousSubscriptionNo, zone));

        if (previousSubscriptionStatus != SubscriptionStatus.ACTIVE) {
            note.add("It was " + previousSubscriptionStatus + " and the new row is ACTIVE: a "
                    + "plan change is somebody buying this school a plan, so the row it lands on "
                    + "is one the school can use.");
        }

        if (previousPeriodEnd != null && !previousPeriodEnd.isAfter(periodStart)) {
            note.add(previousSubscriptionNo + " kept its own end of "
                    + Dates.readable(previousPeriodEnd, zone)
                    + ", because it had already stopped by then — a closed period is only ever "
                    + "shortened, never stretched forward. The gap between that and "
                    + Dates.readable(periodStart, zone) + " is time this school was on nothing.");
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

        String standing = describeSubscriptionState(saved, newPlan, zone);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, newPlan,
                String.join(" ", note));
    }

    //! Endpoint 17 — start the next billing period ------------------------------------
    /** #17 — Renews a subscription for the next billing period with the same terms. */
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

        // Every date this method puts in a message or a note is rendered in the SCHOOL's
        // zone, not UTC: a period starting at midnight in Asia/Kolkata is stored as
        // 18:30Z the day before, so UTC would name the wrong calendar day.
        String zone = school.getDefaultTimeZone();
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
                because = "stopped being sold on "
                        + Dates.readable(plan.getEffectiveUntil());
            } else {
                because = "does not go on sale until "
                        + Dates.readable(plan.getEffectiveFrom());
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
                    subscription.getSubscriptionNo() + " runs to "
                            + Dates.readable(previousPeriodEnd, zone)
                            + ", which has not passed yet, so there is no next period to start. "
                            + "To move that date, use the edit endpoint.");
        }

        //! step 8 - the next period starts exactly where the last one ended, so the two are
        //! contiguous: no day the school was live but unbilled, and none it was billed twice for.
        //! The cycle is the SUBSCRIPTION's, not the plan's — #14 can have changed it, and a
        //! renewal renews what the school is actually on.
        //! The caller's date is required on CUSTOM and refused on the other four, which decide
        //! their own length — so a renewal cannot quietly run to a date the cadence disagrees
        //! with. On CUSTOM it is checked against the new period's start, so a renewal cannot be
        //! made to end before it began.
        Instant periodStart = previousPeriodEnd;
        Instant periodEnd = calculateSubscriptionPeriodEnd(requestedPeriodEnd, periodStart,
                subscription.getBillingCycle(), zone);

        //! step 9 - close the period that just ended. Its dates are left exactly as they are:
        //! it ran its full course, which is the difference between this and #16, where the old
        //! row's end is trimmed to the day the school left the plan.
        String previousSubscriptionNo = subscription.getSubscriptionNo();

        subscription.setCurrent(false);
        subscription.setReasonForChanges("Renewed into the next billing period. This row is the "
                + "period ending " + Dates.readable(previousPeriodEnd, zone) + ".");

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
                        + "period ending " + Dates.readable(previousPeriodEnd, zone)
                        + " and was closed; " + saved.getSubscriptionNo() + " covers "
                        + Dates.readable(periodStart, zone) + " to "
                        + Dates.readable(periodEnd, zone) + ".")
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
                + ", running from " + Dates.readable(periodStart, zone) + " to "
                + Dates.readable(periodEnd, zone) + " on its "
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

        String standing = describeSubscriptionState(saved, plan, zone);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 19 — cut a school off for non-payment ---------------------------------
    /** #19 — Suspends a subscription and the school. */
    @Transactional
    public SubscriptionDetailResponse suspendSubscription(String schoolId, String subscriptionNo,
            SubscriptionSuspendRequest request) {

        //! step 1 - the school has to exist, and be one there is any point cutting off. The same
        //! allow-list #16 and #17 use: a school already closing is not suspended, it is going.
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        // Every date this method puts in a message or a note is rendered in the SCHOOL's
        // zone, not UTC: a period starting at midnight in Asia/Kolkata is stored as
        // 18:30Z the day before, so UTC would name the wrong calendar day.
        String zone = school.getDefaultTimeZone();
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
        note.add("The period was not paused: it still ends "
                + Dates.readable(saved.getCurrentPeriodEnd(), zone)
                + ", so the school is losing time it has paid for. Crediting that is a money "
                + "decision nothing here can make.");

        String standing = describeSubscriptionState(saved, plan, zone);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 20 — switch a school back on after it pays ----------------------------
    /** #20 — Resumes a suspended subscription and the school. */
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

        // Every date this method puts in a message or a note is rendered in the SCHOOL's
        // zone, not UTC: a period starting at midnight in Asia/Kolkata is stored as
        // 18:30Z the day before, so UTC would name the wrong calendar day.
        String zone = school.getDefaultTimeZone();
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
        note.add("The period was NOT extended: it still ends "
                + Dates.readable(saved.getCurrentPeriodEnd(), zone)
                + ", so the school has paid for the time it was locked out of. Crediting that is "
                + "a money decision nothing here can make — moving the date, if that is what was "
                + "agreed, is the edit endpoint.");

        String standing = describeSubscriptionState(saved, plan, zone);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 21 — end the subscription ---------------------------------------------
    /** #21 — Cancels a subscription immediately or at the end of its current period. */
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
        // As everywhere else here: the dates below read in the SCHOOL's zone, because a period
        // end stored as 18:30Z is midnight the next day on that school's own calendar.
        String zone = school.getDefaultTimeZone();

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
                                + "on " + Dates.readable(paidUntil, zone)
                                + ", so there is nothing left to end.");
            }
            if (!request.isImmediate()) {
                throw ApiException.conflict("CANCELLATION_ALREADY_SCHEDULED",
                        subscription.getSubscriptionNo() + " is already cancelled and runs out "
                                + "on " + Dates.readable(paidUntil, zone)
                                + ". Send immediate: true to stop its access now instead.");
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
                                + "for ran to " + Dates.readable(paidUntil, zone)
                                + " and was trimmed to the cancellation. "
                        : "Cancelled from " + previousStatus + " with effect from "
                                + Dates.readable(paidUntil, zone)
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
                    + "from " + Dates.readable(paidUntil, zone) + " to now, which is what stops "
                    + "the access: every feature is refused because the subscription is cancelled "
                    + "AND its period is over.");
            note.add("NO money was refunded for the rest of that period. Nothing here raises or "
                    + "credits an invoice, so what should happen to it is still an open "
                    + "question. The history row keeps the date originally paid for.");
        } else {
            note.add("Cancelled, and the school keeps working until "
                    + Dates.readable(paidUntil, zone) + " — the period it has already paid for. "
                    + "The status says CANCELLED because the contract is over; the period says "
                    + "how long the access lasts.");
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

        String standing = describeSubscriptionState(saved, plan, zone);
        if (standing != null) {
            note.add(standing);
        }

        return SubscriptionDetailResponse.fromSubscription(saved, plan, String.join(" ", note));
    }

    //! Endpoint 27 — what one school is on right now ----------------------------------
    /** #27 — Returns the school's current subscription with plan, pricing, status, and period details. */
    public SubscriptionDetailResponse getSubscription(String schoolId) {

        //! step 1 - the school, read up front rather than only to explain a 404. It is needed
        //! either way now: its timezone is what the dates in the note are rendered in, and a
        //! period end stored as 18:30Z is midnight the next day on that school's own calendar.
        //! The refusals are unchanged — a missing school is still SCHOOL_NOT_FOUND and a school
        //! without a subscription still SUBSCRIPTION_NOT_FOUND, in that order.
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        String zone = school.getDefaultTimeZone();

        //! step 2 - the school's current subscription
        // TODO: read subscription
        SchoolSubscription subscription = schoolSubscription
                .findBySchoolIdAndCurrentIsTrue(schoolId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                        "'" + school.getSchoolName() + "' has no subscription. Create one first."));

        //! step 3 - the plan it points at, for the name, limits and features
        PlanDefinition plan = loadPlanBehindSubscription(subscription);

        return SubscriptionDetailResponse.fromSubscription(
                subscription,
                plan,
                describeSubscriptionState(subscription, plan, zone));
    }
    
    

    //! Endpoint 28 — every subscription this school has ever had ----------------------
    /** #28 — Returns the school's subscription history with filters, sorting, and pagination. */
    public PageResponse<SubscriptionSummaryResponse> listSubscriptions(String schoolId,
            SubscriptionSearchRequest request) {

        //! step 1 - the paging and the order, validated before anything is read. Cheap checks
        //! with no I/O behind them go first, so a malformed request costs no database round trip
        //! — and a 404 for the school is then only ever the answer to an otherwise valid ask.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_SUBSCRIPTION_FIELDS, SORTABLE_SUBSCRIPTION_FIELD_NAMES,
                SUBSCRIPTION_HISTORY_ORDER);

        //! step 2 - a window that runs backwards is a mistake, not an empty result. Mongo would
        //! answer it with zero rows quite happily, and the caller would read that as "this
        //! school has no subscriptions in that range" rather than "you sent from and to the
        //! wrong way round".
        if (request.startDateFrom() != null && request.startDateTo() != null
                && request.startDateFrom().isAfter(request.startDateTo())) {

            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "startDateFrom (" + Dates.readable(request.startDateFrom())
                            + ") must not be after startDateTo ("
                            + Dates.readable(request.startDateTo()) + ").");
        }

        if (request.endDateFrom() != null && request.endDateTo() != null
                && request.endDateFrom().isAfter(request.endDateTo())) {

            throw ApiException.badRequest("INVALID_DATE_RANGE",
                    "endDateFrom (" + Dates.readable(request.endDateFrom())
                            + ") must not be after endDateTo ("
                            + Dates.readable(request.endDateTo()) + ").");
        }

        //! step 3 - the school has to exist. A 404 rather than an empty page: "this school has
        //! no subscriptions" and "there is no such school" are different answers, and a caller
        //! that cannot tell them apart will report the wrong one.
        // TODO: read school
        if (!schools.existsById(schoolId)) {
            throw ApiException.notFound("SCHOOL_NOT_FOUND",
                    "No school found with id '" + schoolId + "'.");
        }

        //! step 4 - resolve a planCode filter to the plan versions it names. The subscription
        //! stores planDefinitionDocsId, not the code, so this is the only way to filter on one —
        //! and it is still one query rather than reading subscriptions and sifting them.
        //!
        //! NULL means no code was sent, so no plan filter at all. An EMPTY set means a code was
        //! sent and matched nothing, which has to match nothing rather than everything.
        Set<String> planIds = null;

        if (request.planCode() != null && !request.planCode().isBlank()) {
            // Shaped the way a code is on the way in, so ?planCode=premium-plus finds
            // PREMIUM_PLUS. Deliberately not the validator: a filter of the wrong shape should
            // match nothing rather than turn a list request into an error.
            String code = request.planCode().trim().toUpperCase()
                    .replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");

            // TODO: read plans (the versions of one code)
            planIds = planDefinition.findByPlanCodeOrderByPlanVersionDesc(code).stream()
                    .map(PlanDefinition::getId)
                    .collect(Collectors.toSet());
        }

        //! step 5 - one page of subscriptions, filtered and ordered in the database
        // TODO: search subscriptions
        Page<SchoolSubscription> page = schoolSubscription.search(schoolId, request, planIds,
                pageable);

        //! step 6 - the plans behind THIS page, in one query. Distinct ids, so a history of
        //! twenty renewals of one plan costs one lookup rather than twenty.
        Set<String> idsOnPage = page.getContent().stream()
                .map(SchoolSubscription::getPlanDefinitionDocsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // TODO: read plans (the ones this page points at)
        Map<String, PlanDefinition> plansById = idsOnPage.isEmpty()
                ? Map.of()
                : planDefinition.findAllById(idsOnPage).stream()
                        .collect(Collectors.toMap(PlanDefinition::getId, Function.identity()));

        //! step 7 - one `now` for the whole page, so periodEnded cannot disagree between the
        //! first row and the last. Two calls to Instant.now() a millisecond apart can straddle a
        //! period end, and a page where one row says ended and another says not is unexplainable.
        Instant now = Instant.now();

        //! step 8 - map, reading each row's plan out of the map rather than fetching it. A plan
        //! that has since been deleted leaves that row's plan fields null; see the DTO.
        return PageResponse.from(page, subscription -> SubscriptionSummaryResponse.fromSubscription(
                subscription, plansById.get(subscription.getPlanDefinitionDocsId()), now));
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
                            + Dates.readable(plan.getEffectiveUntil()) + ".");
        }
        if (plan.getEffectiveFrom() != null && plan.getEffectiveFrom().isAfter(now)) {
            throw ApiException.conflict("PLAN_NOT_SELLABLE",
                    "'" + planCode + "' version " + version + " does not go on sale until "
                            + Dates.readable(plan.getEffectiveFrom()) + ".");
        }
        return plan;
    }

    /**
     * Works out when a billing period ends. THE CADENCE DECIDES WHO SAYS SO, and there are only
     * two answers.
     *
     * <pre>
     * MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY -> the cycle decides: start + 30/90/180/365 days
     * CUSTOM                                  -> the caller decides, and has to send it
     * </pre>
     *
     * <p><b>A fixed cadence refuses an end date rather than honouring it.</b> The four fixed
     * cycles ARE their length: an end date sent with one either agrees with the derivation, in
     * which case it said nothing, or disagrees with it, in which case the record contradicts
     * itself — a subscription reading MONTHLY whose period runs six months bills the school for
     * half a year while the document says it pays every month. There is no third case where the
     * value is useful, so it is a 400 rather than a silent overwrite.
     *
     * <p><b>Refused rather than quietly dropped</b>, which is the harder half of that choice. A
     * caller who sends a date and is answered with a different one has been ignored without being
     * told, and would have to diff the response to notice. Naming the field and the cadence in
     * the refusal is what makes the rule discoverable from one wrong request.
     *
     * <p>The way to move a fixed-cadence period end is to move what it is measured FROM — the
     * start, on #14 — or to change the cadence itself. Both derive a new end from here.
     *
     * <p>Used by:
     * - createSubscription()
     * - updateSubscription()
     * - changePlan()
     * - renewSubscription()
     */
    private Instant calculateSubscriptionPeriodEnd(Instant requested, Instant periodStart,
            BillingCycle cycle, String zone) {

        // CUSTOM has no length of its own, so it is the ONE cadence whose end date is somebody's
        // decision rather than arithmetic. Refused rather than guessed: inventing a year, or
        // repeating the length of the last period, would put a date in a billing record that
        // nobody agreed to.
        if (cycle == BillingCycle.CUSTOM) {
            if (requested == null) {
                throw ApiException.badRequest("BILLING_PERIOD_END_REQUIRED",
                        "currentPeriodEnd has to be sent on a CUSTOM cadence, which has no set "
                                + "length to work the period out from.");
            }
            if (!requested.isAfter(periodStart)) {
                throw ApiException.badRequest("INVALID_BILLING_PERIOD",
                        "currentPeriodEnd (" + Dates.readable(requested, zone)
                                + ") must be after currentPeriodStart ("
                                + Dates.readable(periodStart, zone) + ").");
            }
            return requested;
        }

        // DAYS, so no calendar and no zone is needed: an Instant can add days on its own, where
        // Instant.plus(1, MONTHS) throws because a month is not a fixed number of seconds.
        //
        // Worked out before the refusal below because the message quotes it — a refusal that
        // names the cadence but not its length leaves the caller to guess what they will get
        // instead.
        //
        // CUSTOM is unreachable here: it returned above, because it has no length. An exception
        // rather than a number, so an edit that moves this switch above that block fails loudly
        // instead of billing somebody on a made-up cadence length.
        long days = switch (cycle) {
            case MONTHLY -> 30;
            case QUARTERLY -> 90;
            case HALF_YEARLY -> 180;
            case YEARLY -> 365;
            case CUSTOM -> throw new IllegalStateException(
                    "CUSTOM has no fixed length; its end date comes from the caller.");
        };

        if (requested != null) {
            throw ApiException.badRequest("BILLING_PERIOD_END_NOT_ALLOWED",
                    "currentPeriodEnd cannot be sent on a " + cycle + " cadence, which decides "
                            + "its own: " + cycle + " periods run " + days
                            + " days from currentPeriodStart. Leave it out and it is worked out "
                            + "from the start date; move the start to move the end. Only a "
                            + "CUSTOM cadence takes an end date.");
        }

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
    private String describeSubscriptionState(SchoolSubscription subscription, PlanDefinition plan,
            String zone) {
        List<String> notes = new ArrayList<>();

        Instant end = subscription.getCurrentPeriodEnd();
        boolean live = subscription.getStatus() == SubscriptionStatus.ACTIVE
                || subscription.getStatus() == SubscriptionStatus.TRIAL;

        if (end != null && !end.isAfter(Instant.now()) && live) {
            notes.add("The period ended on " + Dates.readable(end, zone) + " but the status "
                    + "still says " + subscription.getStatus() + ". Nothing marks a subscription "
                    + "expired on its own yet, so this has to be read as lapsed rather than "
                    + "paying — renewing it starts the next period.");
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
            SchoolSubscription saved, String previousSubscriptionNo, String zone) {

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
                + ", running from today to "
                + Dates.readable(saved.getCurrentPeriodEnd(), zone) + " on the new plan's "
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
                    "currentPeriodStart ("
                            + Dates.readable(requestedStart, school.getDefaultTimeZone())
                            + ") is before the start of today in the school's timezone ("
                            + Dates.readable(startOfToday, school.getDefaultTimeZone())
                            + "). A billing period starts today or later — nothing here can "
                            + "invoice a period that has already run.");
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
    private String describeCreateOutcome(SchoolSubscription subscription, boolean trial,
            String activation, String zone) {
        String base = trial
                ? "Trial started, running to "
                        + Dates.readable(subscription.getCurrentPeriodEnd(), zone) + "."
                : "Subscribed, and billed from "
                        + Dates.readable(subscription.getCurrentPeriodStart(), zone) + ".";

        return base + activation + " No invoice has been raised: that is a separate step.";
    }
}
