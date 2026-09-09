package com.orbitastra.backend.common.access;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;

import lombok.RequiredArgsConstructor;

/**
 * The three questions that come before any school action: is the school live, is it paying, and
 * is its year open.
 *
 * <p><b>NOT WIRED IN ANYWHERE YET.</b> Nothing calls these methods. They are here so that when
 * the modules that need gating arrive, the answer is one shared decision instead of one per
 * module — which is the whole point, because three modules that each work out "may this school
 * act" will eventually disagree, and the disagreement will look like a bug in whichever one is
 * stricter.
 *
 * <p>In {@code common} rather than in a service because none of the three modules owns the
 * question. {@code CurrentSchoolResolver} sits here for the same reason.
 *
 * <h2>They throw rather than answer</h2>
 *
 * <p>Each {@code require...} method returns normally or throws. That is deliberate: a gate whose
 * answer can be ignored is a gate somebody forgets to check, and {@code if (!gate.ok(x)) return;}
 * is one missing {@code !} away from letting everything through. The refusal carries the reason,
 * so a caller never has to build the message.
 *
 * <p><b>409 rather than 403</b>, matching what this project already returns for "the state
 * forbids this" — {@code SCHOOL_NOT_SUBSCRIBABLE}, {@code PLAN_UNCHANGED},
 * {@code PERIOD_NOT_ENDED} are all conflicts. Nothing here is about who the caller is, which is
 * what 403 would say; it is about what the record currently permits.
 *
 * <h2>Every date in a refusal is spelled out</h2>
 *
 * <p>Through {@link Dates}, in the <b>school's own timezone</b>, because a period that ends at
 * midnight in Asia/Kolkata is stored as 18:30Z the day before and UTC would name the wrong day.
 * See the project rule beside {@code Dates}.
 *
 * <h2>What this does NOT do</h2>
 *
 * <p>It does not check feature access or limits — that is #34, and it is a different question
 * with a different answer per feature. These three are the gate in front of it: there is no point
 * asking whether a school may use a feature if the school is closed.
 */
@Component
@RequiredArgsConstructor
public class ActionGate {

    private final SchoolRepository schools;
    private final SchoolSubscriptionRepository subscriptions;

    //! Gate 1 — is the school itself live ---------------------------------------------

    /**
     * Refuses unless the school is {@code ACTIVE}.
     *
     * <p>Reads the school, so the caller gets it back rather than loading it twice — every caller
     * of this needs the school for something anyway.
     *
     * <p><b>{@code PROVISIONING} is told apart from the rest on purpose</b>, and gets its own
     * code and a different tone. It is the state every school starts in and the only one that
     * ends by itself: setup finishes, the school goes live, and the action the caller just tried
     * starts working. Telling somebody "not allowed" for a state that resolves on its own reads
     * as a rejection when it is a wait.
     *
     * <p>The other five are refusals, and each says which one it is, because they mean genuinely
     * different things to whoever reads them — {@code SUSPENDED} comes back, {@code DELETED} does
     * not.
     *
     * @return the school, which is {@code ACTIVE}
     * @throws ApiException 404 {@code SCHOOL_NOT_FOUND} when there is no such school,
     *                      409 {@code SCHOOL_NOT_READY} while it is still being set up,
     *                      409 {@code SCHOOL_NOT_ACTIVE} for the five blocked states
     */
    public School requireActiveSchool(String schoolId) {
        //! step 1 - the school has to exist at all
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - and be live
        return requireActiveSchool(school);
    }

    /**
     * The same check on a school already in hand, so a caller that has just read one does not
     * read it again.
     *
     * <p>Both forms exist because the callers differ: a platform endpoint has the id from its
     * URL, and a school-surface endpoint already has the school from
     * {@code CurrentSchoolResolver}.
     *
     * @return the school it was given, which is {@code ACTIVE}
     */
    public School requireActiveSchool(School school) {
        SchoolStatus status = school.getStatus();

        if (status == SchoolStatus.ACTIVE) {
            return school;
        }

        String name = school.getSchoolName();

        //! step 1 - still being set up, which is a wait rather than a refusal
        if (status == SchoolStatus.PROVISIONING) {
            throw ApiException.conflict("SCHOOL_NOT_READY",
                    "'" + name + "' is still being set up, so this cannot be done yet. Nothing "
                            + "is wrong — the last of the setup is finishing, and everything "
                            + "opens up the moment the school goes live.");
        }

        //! step 2 - the five that are genuinely closed, each saying which
        String because = switch (status) {
            case SUSPENDED -> "its access is temporarily blocked. It comes back as soon as the "
                    + "school is switched on again.";
            case OFFBOARDING -> "it is being closed down — data export and contract closure are "
                    + "in progress.";
            case CLOSED -> "it is closed. The records are kept under the retention rules, but "
                    + "nothing can be changed.";
            case DELETION_PENDING -> "permanent deletion has been requested and has not run yet. "
                    + "Nothing may be changed in the meantime.";
            case DELETED -> "it has been deleted.";
            // ACTIVE returned above; PROVISIONING threw above. Listed so that adding a status to
            // the enum fails to compile here rather than falling through to a wrong answer.
            case ACTIVE, PROVISIONING -> throw new IllegalStateException(
                    "Handled before this switch: " + status);
        };

        throw ApiException.conflict("SCHOOL_NOT_ACTIVE",
                "'" + name + "' cannot do this because " + because);
    }

    //! Gate 2 — is the school paying ---------------------------------------------------

    /**
     * Refuses unless the school's current subscription still grants the product.
     *
     * <p>Reads the school for its name and timezone, then its current subscription.
     *
     * @return the subscription, which grants
     * @throws ApiException 404 {@code SCHOOL_NOT_FOUND} / {@code SUBSCRIPTION_NOT_FOUND},
     *                      409 {@code SUBSCRIPTION_NOT_USABLE}
     */
    public SchoolSubscription requireUsableSubscription(String schoolId) {
        //! step 1 - the school, for the name and the zone the dates are read in
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - the subscription, and whether it still grants
        return requireUsableSubscription(school);
    }

    /**
     * The same check for a school already in hand, so a caller that has just resolved one does
     * not read it again.
     *
     * <p>This is the form the school surface wants: {@code CurrentSchoolResolver} has already
     * produced the school, and it carries both things the refusal messages need — the name and
     * the timezone.
     *
     * @return the subscription, which grants
     */
    public SchoolSubscription requireUsableSubscription(School school) {
        //! step 1 - the one row it is on now. A school with none has never bought anything.
        // TODO: read school subscription
        SchoolSubscription subscription = subscriptions
                .findBySchoolIdAndCurrentIsTrue(school.getId())
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND",
                        "'" + school.getSchoolName() + "' has no subscription."));

        //! step 2 - and it has to still grant
        return requireUsableSubscription(subscription, school.getSchoolName(),
                school.getDefaultTimeZone());
    }

    /**
     * The same check on a subscription already in hand.
     *
     * <h2>What grants, and why</h2>
     *
     * <p><b>{@code TRIAL} and {@code ACTIVE}</b> grant. Nothing to explain.
     *
     * <p><b>{@code PAST_DUE} grants</b>, and this is the one worth stating. An unpaid invoice is a
     * conversation, not a reason to lock a school out of its attendance register in the middle of
     * the morning. It is also what the rest of the codebase already decided: see the javadoc on
     * {@link SubscriptionStatus#PAST_DUE} and {@code SchoolSubscriptionServiceUtils.whyNotActive},
     * which grants it too. If it should ever stop granting, the right shape is a <b>grace
     * period</b> — "past due for more than N days" — not a status check, because the status alone
     * cannot tell an invoice a week late from one six months late.
     *
     * <p><b>{@code CANCELLED} grants until its period runs out.</b> A school cancelling mid-month
     * has bought that month, and refusing it the same afternoon would be keeping its money and
     * taking the product away. So a cancellation is not checked as a status at all — it falls
     * through to the period check below. #21's immediate shape works by trimming
     * {@code currentPeriodEnd} to now, which is what makes that check bite at once; no extra
     * field says "cancelled but still running", because the status says cancelled and the dates
     * say how long for.
     *
     * <p><b>{@code SUSPENDED} and {@code EXPIRED} refuse outright.</b>
     *
     * <p><b>A period that has ended refuses whatever the status says</b>, and that check is last
     * so it catches everything. It has to exist separately, because nothing marks a lapsed
     * subscription {@code EXPIRED} yet — a row can read {@code ACTIVE} with a period that
     * finished months ago, and trusting the status alone would grant it.
     *
     * <p><b>The rules here are the same ones {@code whyNotActive} applies</b>, which returns them
     * as a sentence for #33's response rather than throwing. They must not drift: when this gate
     * is wired in, that method should call it rather than keep its own copy. Left alone for now
     * because nothing may be rewired yet.
     *
     * @param schoolName for the message; a refusal that does not name the school is hard to act on
     * @param zone       the school's {@code defaultTimeZone}, so a date reads as its own calendar
     * @return the subscription it was given, which grants
     */
    public SchoolSubscription requireUsableSubscription(SchoolSubscription subscription,
            String schoolName, String zone) {

        SubscriptionStatus status = subscription.getStatus();
        Instant periodEnd = subscription.getCurrentPeriodEnd();

        //! step 1 - the two statuses that refuse on their own, whatever the dates say
        if (status == SubscriptionStatus.SUSPENDED) {
            throw ApiException.conflict("SUBSCRIPTION_NOT_USABLE",
                    "'" + schoolName + "' cannot do this because its subscription ("
                            + subscription.getSubscriptionNo() + ") is suspended.");
        }

        if (status == SubscriptionStatus.EXPIRED) {
            throw ApiException.conflict("SUBSCRIPTION_NOT_USABLE",
                    "'" + schoolName + "' cannot do this because its subscription ("
                            + subscription.getSubscriptionNo() + ") has expired. Renewing it "
                            + "starts the next period.");
        }

        //! step 2 - a cancellation that has run out. Said separately from the plain period check
        //! below so the message explains WHY the period will not move: it was cancelled, so
        //! nothing is going to renew it on its own.
        if (status == SubscriptionStatus.CANCELLED
                && periodEnd != null && !periodEnd.isAfter(Instant.now())) {

            throw ApiException.conflict("SUBSCRIPTION_NOT_USABLE",
                    "'" + schoolName + "' cannot do this because its subscription ("
                            + subscription.getSubscriptionNo() + ") was cancelled and its period "
                            + "ended on " + Dates.readable(periodEnd, zone) + ".");
        }

        //! step 3 - and a period that has ended, whatever the status claims. Last on purpose:
        //! this is the check that catches an ACTIVE row nobody has marked expired.
        if (periodEnd != null && !periodEnd.isAfter(Instant.now())) {
            throw ApiException.conflict("SUBSCRIPTION_NOT_USABLE",
                    "'" + schoolName + "' cannot do this because its subscription period ended "
                            + "on " + Dates.readable(periodEnd, zone) + ". Renewing it starts "
                            + "the next period.");
        }

        //! step 4 - TRIAL, ACTIVE, PAST_DUE, and a CANCELLED row still inside its period
        return subscription;
    }

    //! Gate 3 — is the school's year open ---------------------------------------------

    /**
     * Refuses unless the academic year is the one the school is running, and today is inside it.
     *
     * <p><b>Both conditions, and the order of that sentence matters.</b> The dates are
     * authoritative — #18 and #19 refuse overlapping years, so at most one year contains any
     * given date. The flag only ever <b>narrows</b> what the dates already permit: it says the
     * school has switched over to a year the calendar says has begun, which the dates cannot say
     * on their own. Reading the flag alone would be the mistake, because one left true after its
     * year ended would keep a finished year live indefinitely. See the note on
     * {@code AcademicYear.isThisYearRunning}, which records the objection to storing this at all.
     *
     * <p><b>A null flag counts as not running</b>, which is what the documents predating the
     * field read as. A gate should fail closed.
     *
     * <p><b>Today is today in the school's own zone.</b> At 23:00 in Asia/Kolkata it is still
     * yesterday in UTC, so a year ending today would already read as finished — which is a whole
     * day of the school's work refused.
     *
     * <p>Both boundaries are <b>inclusive</b>: {@code startDate} is the first school day and
     * {@code endDate} the last, so a year running 1 April to 31 March grants on both of those
     * days.
     *
     * @param zone the school's {@code defaultTimeZone}
     * @return the year it was given, which is open
     * @throws ApiException 409 {@code ACADEMIC_YEAR_NOT_RUNNING}
     */
    public AcademicYear requireRunningAcademicYear(AcademicYear year, String zone) {
        //! step 1 - the school has to have marked this year as the one it is operating in
        if (!Boolean.TRUE.equals(year.getIsThisYearRunning())) {
            throw ApiException.conflict("ACADEMIC_YEAR_NOT_RUNNING",
                    "Academic year '" + year.getName() + "' is not the year this school is "
                            + "running, so this cannot be recorded against it.");
        }

        LocalDate today = Dates.todayIn(zone);

        //! step 2 - and today has to be inside it. Inclusive at both ends.
        if (today.isBefore(year.getStartDate())) {
            throw ApiException.conflict("ACADEMIC_YEAR_NOT_RUNNING",
                    "Academic year '" + year.getName() + "' has not started yet — it begins on "
                            + Dates.readable(year.getStartDate()) + ".");
        }

        if (today.isAfter(year.getEndDate())) {
            throw ApiException.conflict("ACADEMIC_YEAR_NOT_RUNNING",
                    "Academic year '" + year.getName() + "' finished on "
                            + Dates.readable(year.getEndDate())
                            + ", so nothing more can be recorded against it.");
        }

        //! step 3 - marked as running, and today is inside its dates
        return year;
    }
}
