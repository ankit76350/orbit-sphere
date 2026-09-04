package com.orbitastra.backend.services.plans;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionActivateRequest;
import com.orbitastra.backend.dto.plans.subscription.MySubscriptionResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionDetailResponse;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionCreateRequest;
import com.orbitastra.backend.dto.plans.subscription.SubscriptionResponse;
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

        return SubscriptionResponse.fromSubscription(savedSubscription, plan, nextStepFor(school,
                savedSubscription, trial));
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
            case PROVISIONING, TRIAL -> " The school itself is still " + school.getStatus()
                    + " — activating the subscription does not activate the school.";
            case SUSPENDED -> " The school itself is still SUSPENDED — paying does not lift a "
                    + "suspension.";
            default -> "";
        };

        return base + activation + " No invoice has been raised: that is a separate step.";
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
     * What the caller should know next — including, where it applies, that this school is now
     * one {@code activateSchool} will accept without complaint.
     */
    private String nextStepFor(School school, SchoolSubscription subscription, boolean trial) {
        String base = trial
                ? "Trial started, running to " + subscription.getCurrentPeriodEnd() + "."
                : "Subscribed, and billed from " + subscription.getCurrentPeriodStart() + ".";

        String activation = switch (school.getStatus()) {
            case PROVISIONING, TRIAL -> " The school can now be activated, and this is the "
                    + "subscription its activation check was written to look for.";
            case SUSPENDED -> " The school itself is still SUSPENDED — a subscription does not "
                    + "reactivate it.";
            default -> "";
        };

        return base + activation + " No invoice has been raised: that is a separate step.";
    }
}
