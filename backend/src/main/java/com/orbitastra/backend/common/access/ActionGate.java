package com.orbitastra.backend.common.access;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
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
 * Central access gate for school actions.
 *
 * <p>Checks three conditions before an action is allowed:
 * whether the school is active, whether its current subscription is usable, and whether the
 * academic year is running.
 *
 * <p>Each {@code require...} method either returns the valid object or throws an
 * {@link ApiException} with the reason the action is not allowed.
 *
 * <p>This class is kept in {@code common.access} because these checks are shared across modules
 * and should follow the same rules everywhere.
 *
 * <p>Subscription and school status checks use {@code 409 Conflict} when the current state
 * prevents the action. Missing schools or subscriptions return {@code 404 Not Found}.
 *
 * <p>Date-based checks use the school's {@code defaultTimeZone} through {@link Dates} so that
 * dates and periods are evaluated using the school's local calendar.
 *
 * <p>This class only checks school, subscription, and academic-year access. Feature access and
 * usage limits are handled separately by #34.
 */
@Component
@RequiredArgsConstructor
public class ActionGate {

    private final SchoolRepository schools;
    private final SchoolSubscriptionRepository subscriptions;

    //! Gate 1 — is the school itself live ---------------------------------------------
    /** Refuses unless the school is {@code ACTIVE} and returns the school. */
    public School requireActiveSchool(String schoolId) {
        //! step 1 - the school has to exist at all
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - and be live
        return requireActiveSchool(school);
    }

    /** Checks whether the given school is {@code ACTIVE}. */
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

    //! Gate 2 — is the school paying --------------------------------------------------
    /** Checks whether the school's current subscription is usable. */
    public SchoolSubscription requireUsableSubscription(String schoolId) {
        //! step 1 - the school, for the name and the zone the dates are read in
        // TODO: read school
        School school = schools.findById(schoolId)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_NOT_FOUND",
                        "No school found with id '" + schoolId + "'."));

        //! step 2 - the subscription, and whether it still grants
        return requireUsableSubscription(school);
    }

    /** Checks whether the given school's current subscription is usable. */
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

    /** Checks whether the subscription is usable and its period is still active. */
    public SchoolSubscription requireUsableSubscription(SchoolSubscription subscription,
            String schoolName, SchoolTimeZone zone) {

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
    /** Checks whether the academic year is currently running and today's date is within its range. */
    public AcademicYear requireRunningAcademicYear(AcademicYear year, SchoolTimeZone zone) {
        //! step 1 - the school has to have marked this year as the one it is operating in.
        //! Through gate 4 rather than repeated here, so the flag rule has exactly one home and
        //! the two gates can never come to disagree about what "running" means.
        requireYearMarkedAsRunning(year);

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

    //! Gate 4 — is this the school's working year, whatever the calendar says ---------
    /** Checks whether the academic year is marked as the school's current working year. */
    public AcademicYear requireYearMarkedAsRunning(AcademicYear year) {
        //! step 1 - true and nothing else. Null and false are both refusals.
        if (!Boolean.TRUE.equals(year.getIsThisYearRunning())) {
            throw ApiException.conflict("ACADEMIC_YEAR_NOT_RUNNING",
                    "Academic year '" + year.getName() + "' is not the year this school is "
                            + "running, so this cannot be recorded against it."
                            + (year.getIsThisYearRunning() == null
                                    ? " That year predates the flag, so the record does not say"
                                            + " either way — mark it as running first."
                                    : ""));
        }

        //! step 2 - marked as running
        return year;
    }
}
