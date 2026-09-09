package com.orbitastra.backend.services.plans.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionUpdateRequest;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.PlanStatus;
import com.orbitastra.backend.models.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;
import com.orbitastra.backend.repositories.plans.plandefinition.PlanDefinitionRepository;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;
import com.orbitastra.backend.services.core.SchoolPlatformService;
import com.orbitastra.backend.services.plans.helper.PlansHelper;

import lombok.RequiredArgsConstructor;

/**
 * The shared bits every platform subscription endpoint needs.
 *
 * <p>Moved out of {@code PlatformSubscriptionService} so that file holds the endpoints and
 * nothing else. It had fifteen private helpers sitting under its last endpoint, so finding the
 * next thing that answers a request meant scrolling past all of them.
 *
 * <p>Same idea as {@code AcademicYearServiceUtils} in core, and a {@code @Component} for the
 * same reason: some of these need a repository, so they cannot be static.
 *
 * <p>Nothing here decides what an endpoint does. These look a plan up, work out when a period
 * ends, write the request onto a subscription, and turn what happened into a sentence. The
 * refusals they throw are about the values they were given, not about the endpoint's rules.
 *
 * <p>Every method says which endpoints use it, in the note above it.
 */
@Component
@RequiredArgsConstructor
public class PlatformSubscriptionServiceUtils {

    private final SchoolRepository schools;
    private final PlanDefinitionRepository planDefinition;
    private final SchoolSubscriptionRepository schoolSubscription;
    private final PlansHelper helper;
    private final SchoolPlatformService schoolPlatform;

    /** URL value used to refer to the school's current subscription. */
    private static final String CURRENT_SUBSCRIPTION = "current";

    /**
     * What a Mongo document id looks like: 24 hex characters.
     *
     * <p>Matched rather than parsed, because {@code new ObjectId(s)} throws on anything else and
     * a path segment that is not an id is an ordinary 404, not an exception to catch.
     */
    private static final Pattern OBJECT_ID = Pattern.compile("^[0-9a-fA-F]{24}$");

    /** The plan version a school can be assigned to today; drafts and retired plans are rejected, while private quotes remain valid.      *
     * Used by:
     * - createSubscription()
     */
    public PlanDefinition loadSellablePlan(String code, Integer version) {
        String planCode = helper.normalizePlanCode(code);
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
    public Instant calculateSubscriptionPeriodEnd(Instant requested, Instant periodStart,
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

    /**
     * Anything about this subscription worth saying out loud.
     *
     * <p>Each of these is a state the module can genuinely be in today, and each one would
     * otherwise be read wrongly off a single field.
     *
     * Used by:
     * - getSubscription()
     * - updateSubscription()
     */
    public String describeSubscriptionState(SchoolSubscription subscription, PlanDefinition plan,
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
     * <p>Three things are accepted in the one path segment, and they cannot be confused with each
     * other:
     *
     * <ol>
     * <li><b>{@code current}</b> — the row the school is on now. See
     *     {@link #CURRENT_SUBSCRIPTION} for why the word is needed: a subscription number has
     *     slashes in it and cannot be written in a path at all.</li>
     * <li><b>The subscription's own id</b> — 24 hex characters, which is what #28 returns as
     *     {@code subscriptionId} precisely so that a row from a list can be asked about
     *     directly. A subscription number is never that shape, so there is nothing to
     *     disambiguate.</li>
     * <li><b>The subscription number</b> — kept because it is the number printed on the record,
     *     and it does resolve for a caller that can send it (a test, an internal call, or a
     *     number that one day has no slashes in it).</li>
     * </ol>
     *
     * <p><b>The school id is in every one of those lookups.</b> Even the id lookup, which is
     * globally unique on its own: it is the tenant boundary, so a caller who guesses another
     * school's subscription id gets a 404 rather than somebody else's record.
     *
     * Used by:
     * - cancelSubscription()
     * - changePlan()
     * - getSubscriptionHistory()
     * - renewSubscription()
     * - resumeSubscription()
     * - suspendSubscription()
     * - updateSubscription()
     */
    public SchoolSubscription findSchoolSubscription(School school, String schoolId,
            String subscriptionNo) {

        if (CURRENT_SUBSCRIPTION.equalsIgnoreCase(subscriptionNo)) {
            // TODO: read school subscription
            return schoolSubscription.findBySchoolIdAndCurrentIsTrue(schoolId)
                    .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                            "'" + school.getSchoolName() + "' has no subscription yet. Create "
                                    + "one first."));
        }

        // Shaped like a Mongo id, so it is one. Checked here rather than attempted-and-fallen-back
        // so that exactly one query runs whichever form was sent.
        if (subscriptionNo != null && OBJECT_ID.matcher(subscriptionNo).matches()) {
            // TODO: read school subscription
            return schoolSubscription.findBySchoolIdAndId(schoolId, subscriptionNo)
                    .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                            "'" + school.getSchoolName() + "' has no subscription with id '"
                                    + subscriptionNo + "'."));
        }

        // TODO: read school subscription
        return schoolSubscription.findBySchoolIdAndSubscriptionNo(schoolId, subscriptionNo)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                        "'" + school.getSchoolName() + "' has no subscription numbered '"
                                + subscriptionNo + "'. A subscription number contains slashes "
                                + "and cannot be written in a URL — use 'current', or the "
                                + "subscriptionId from the subscription list."));
    }

    /**
     * One plan out of a page's worth, tolerating a row that names no plan.
     *
     * <p><b>This exists because {@code Map.of().get(null)} throws.</b> Both list endpoints fetch
     * the plans behind a page in one query and then read each row's plan out of the map, and both
     * have rows whose plan id can be null — a history row for an event that moved no plan, or a
     * subscription whose plan link is missing. When <i>no</i> row on the page names a plan the map
     * is the empty one, and {@code Map.of()} is {@code ImmutableCollections.MapN}, which
     * {@code requireNonNull}s the key rather than answering null like {@code HashMap} does. So the
     * page that needed no plan lookup at all was the one that failed with a
     * {@code NullPointerException}.
     *
     * <p>Found by #29 against six audit rows that name no plan. #28 had the same latent fault:
     * its map is built the same way, and its {@code Objects::nonNull} filter says the author
     * already knew a subscription's plan id could be absent.
     *
     * <p>Checking the id here rather than choosing a map type is deliberate — it cannot be undone
     * by somebody tidying an empty {@code HashMap} back into {@code Map.of()}.
     *
     * @param plansById the plans behind this page, keyed by id
     * @param planDocsId the plan this row points at, or null when it points at none
     * @return the plan, or null when the row names none or that plan document has gone
     *
     * Used by:
     * - getSubscriptionHistory()
     * - listSubscriptions()
     */
    public PlanDefinition planFrom(Map<String, PlanDefinition> plansById, String planDocsId) {
        return planDocsId == null ? null : plansById.get(planDocsId);
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
    public List<String> applySubscriptionEdits(SchoolSubscription subscription,
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
    public SubscriptionEventType chooseHistoryEventType(SubscriptionStatus previousStatus,
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
    public String buildHistoryReason(List<String> changed, String callerReason) {
        String fields = "Edited " + String.join(", ", changed) + ".";

        return callerReason == null || callerReason.isBlank()
                ? fields
                : fields + " " + callerReason.trim();
    }

    /** The plan a subscription points at, which must exist for the response to be complete.      *
     * Used by:
     * - getSubscription()
     * - updateSubscription()
     */
    public PlanDefinition loadPlanBehindSubscription(SchoolSubscription subscription) {
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
    public String describePlanMove(PlanDefinition previousPlan, PlanDefinition newPlan,
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
    public String describeEditOutcome(SchoolSubscription subscription, PlanDefinition plan,
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
     * - changePlan()
     * - createSubscription()
     */
    public void validateCapacityOverrideIsAtLeastOne(String label, Long value) {
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
     * - changePlan()
     * - createSubscription()
     * - updateSubscription()
     */
    public void validatePeriodStartIsTodayOrLater(Instant requestedStart, School school) {
        if (requestedStart == null) {
            return;
        }

        // Dates owns this: a utils method must not call another utils method, and a rendered
        // date and a date comparison must not disagree about which zone they used.
        Instant startOfToday = Dates.startOfTodayIn(school.getDefaultTimeZone());

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
    public String activateSchoolIfSetupComplete(School school) {
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
    public String describeCreateOutcome(SchoolSubscription subscription, boolean trial,
            String activation, String zone) {
        String base = trial
                ? "Trial started, running to "
                        + Dates.readable(subscription.getCurrentPeriodEnd(), zone) + "."
                : "Subscribed, and billed from "
                        + Dates.readable(subscription.getCurrentPeriodStart(), zone) + ".";

        return base + activation + " No invoice has been raised: that is a separate step.";
    }
}
