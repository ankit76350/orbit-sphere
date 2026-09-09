package com.orbitastra.backend.common.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;

/**
 * Every branch of the three gates.
 *
 * <p>Written even though nothing calls them yet, because a gate is exactly the kind of code whose
 * bugs are invisible: one that wrongly grants looks like everything working, and it is only found
 * by whatever it should have stopped.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionGateTest {

    private static final String SCHOOL_ID = "6aa15d9dc3f7d0033333333a";
    private static final String KOLKATA = "Asia/Kolkata";

    @Mock private SchoolRepository schools;
    @Mock private SchoolSubscriptionRepository subscriptions;
    @InjectMocks private ActionGate gate;

    private School school(SchoolStatus status) {
        School s = new School();
        s.setId(SCHOOL_ID);
        s.setSchoolName("Green Valley");
        s.setDefaultTimeZone(KOLKATA);
        s.setStatus(status);
        return s;
    }

    private SchoolSubscription subscription(SubscriptionStatus status, Instant periodEnd) {
        SchoolSubscription s = new SchoolSubscription();
        s.setSchoolId(SCHOOL_ID);
        s.setSubscriptionNo("SUB/2026/09/000001");
        s.setStatus(status);
        s.setCurrentPeriodEnd(periodEnd);
        s.setCurrent(true);
        return s;
    }

    /** `name` has no setter — it is immutable by design — so everything goes via the builder. */
    private AcademicYear year(boolean running, LocalDate start, LocalDate end) {
        return AcademicYear.builder()
                .name("2026-2027")
                .startDate(start)
                .endDate(end)
                .isThisYearRunning(running)
                .build();
    }

    // ============================================================ gate 1: the school

    @Nested
    @DisplayName("the school has to be live")
    class TheSchool {

        @Test
        @DisplayName("ACTIVE passes, and hands the school back")
        void activePasses() {
            School active = school(SchoolStatus.ACTIVE);

            assertThat(gate.requireActiveSchool(active)).isSameAs(active);
        }

        @Test
        @DisplayName("PROVISIONING is a wait, not a refusal, and says so")
        void provisioningReadsAsAWait() {
            assertThatThrownBy(() -> gate.requireActiveSchool(school(SchoolStatus.PROVISIONING)))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Green Valley")
                    .hasMessageContaining("still being set up")
                    // The tone is the point: nothing is wrong, it is nearly ready.
                    .hasMessageContaining("Nothing is wrong")
                    .hasMessageContaining("opens up the moment the school goes live");
        }

        @Test
        @DisplayName("and PROVISIONING has its own code, so a client can tell a wait from a block")
        void provisioningHasItsOwnCode() {
            assertThatThrownBy(() -> gate.requireActiveSchool(school(SchoolStatus.PROVISIONING)))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo("SCHOOL_NOT_READY");
        }

        @ParameterizedTest
        @EnumSource(value = SchoolStatus.class,
                names = {"SUSPENDED", "OFFBOARDING", "CLOSED", "DELETION_PENDING", "DELETED"})
        @DisplayName("each blocked status is refused as SCHOOL_NOT_ACTIVE")
        void blockedStatusesAreRefused(SchoolStatus status) {
            assertThatThrownBy(() -> gate.requireActiveSchool(school(status)))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo("SCHOOL_NOT_ACTIVE");
        }

        @ParameterizedTest
        @EnumSource(value = SchoolStatus.class,
                names = {"SUSPENDED", "OFFBOARDING", "CLOSED", "DELETION_PENDING", "DELETED"})
        @DisplayName("and every one of them names the school and gives its own reason")
        void everyBlockedStatusExplainsItself(SchoolStatus status) {
            assertThatThrownBy(() -> gate.requireActiveSchool(school(status)))
                    .hasMessageContaining("Green Valley")
                    .hasMessageContaining("cannot do this because");
        }

        @Test
        @DisplayName("the five reasons are all different from each other")
        void theReasonsAreDistinct() {
            var messages = new java.util.HashSet<String>();
            for (SchoolStatus status : new SchoolStatus[] {SchoolStatus.SUSPENDED,
                    SchoolStatus.OFFBOARDING, SchoolStatus.CLOSED, SchoolStatus.DELETION_PENDING,
                    SchoolStatus.DELETED}) {
                try {
                    gate.requireActiveSchool(school(status));
                } catch (ApiException e) {
                    messages.add(e.getMessage());
                }
            }
            // A shared message would make five different situations look like one.
            assertThat(messages).hasSize(5);
        }

        @Test
        @DisplayName("SUSPENDED says it comes back; DELETED does not")
        void temporaryAndPermanentReadDifferently() {
            assertThatThrownBy(() -> gate.requireActiveSchool(school(SchoolStatus.SUSPENDED)))
                    .hasMessageContaining("comes back");
            assertThatThrownBy(() -> gate.requireActiveSchool(school(SchoolStatus.DELETED)))
                    .hasMessageNotContaining("comes back");
        }

        @Test
        @DisplayName("by id, it reads the school first")
        void byIdReadsTheSchool() {
            when(schools.findById(SCHOOL_ID)).thenReturn(Optional.of(school(SchoolStatus.ACTIVE)));

            assertThat(gate.requireActiveSchool(SCHOOL_ID).getSchoolName())
                    .isEqualTo("Green Valley");
        }

        @Test
        @DisplayName("a school that does not exist is a 404, not a block")
        void missingSchoolIsNotFound() {
            when(schools.findById(SCHOOL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gate.requireActiveSchool(SCHOOL_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo("SCHOOL_NOT_FOUND");
        }
    }

    // ============================================================ gate 2: the subscription

    @Nested
    @DisplayName("the subscription has to still grant")
    class TheSubscription {

        private final Instant future = Instant.now().plus(30, ChronoUnit.DAYS);
        private final Instant past = Instant.now().minus(30, ChronoUnit.DAYS);

        private void check(SubscriptionStatus status, Instant end) {
            gate.requireUsableSubscription(subscription(status, end), "Green Valley", KOLKATA);
        }

        @Test
        @DisplayName("TRIAL grants")
        void trialGrants() {
            assertThatCode(() -> check(SubscriptionStatus.TRIAL, future)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE grants")
        void activeGrants() {
            assertThatCode(() -> check(SubscriptionStatus.ACTIVE, future)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PAST_DUE grants — an unpaid invoice is a conversation, not a lockout")
        void pastDueGrants() {
            // The same position as SubscriptionStatus.PAST_DUE's javadoc and whyNotActive. If this
            // ever has to change, the shape is a grace period, not a status check.
            assertThatCode(() -> check(SubscriptionStatus.PAST_DUE, future))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CANCELLED grants while the period it was paid for is still running")
        void cancelledGrantsInsideItsPeriod() {
            // A school cancelling mid-month bought that month. Refusing it the same afternoon
            // would be keeping its money and taking the product away.
            assertThatCode(() -> check(SubscriptionStatus.CANCELLED, future))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CANCELLED refuses once that period has ended, and says it was cancelled")
        void cancelledRefusesAfterItsPeriod() {
            assertThatThrownBy(() -> check(SubscriptionStatus.CANCELLED, past))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("was cancelled")
                    .hasMessageContaining("period ended on");
        }

        @Test
        @DisplayName("SUSPENDED refuses whatever the dates say")
        void suspendedRefuses() {
            assertThatThrownBy(() -> check(SubscriptionStatus.SUSPENDED, future))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("is suspended");
        }

        @Test
        @DisplayName("EXPIRED refuses whatever the dates say")
        void expiredRefuses() {
            assertThatThrownBy(() -> check(SubscriptionStatus.EXPIRED, future))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("has expired");
        }

        @Test
        @DisplayName("a period that has ended refuses even a row still marked ACTIVE")
        void aLapsedPeriodRefusesAnActiveRow() {
            // Nothing marks a lapsed subscription EXPIRED yet, so this is the check that catches
            // it. Without it, an ACTIVE row from two years ago would still grant.
            assertThatThrownBy(() -> check(SubscriptionStatus.ACTIVE, past))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("subscription period ended on");
        }

        @Test
        @DisplayName("and it refuses a lapsed TRIAL and a lapsed PAST_DUE too")
        void aLapsedPeriodRefusesTheOtherGrantingStatuses() {
            assertThatThrownBy(() -> check(SubscriptionStatus.TRIAL, past))
                    .hasMessageContaining("period ended on");
            assertThatThrownBy(() -> check(SubscriptionStatus.PAST_DUE, past))
                    .hasMessageContaining("period ended on");
        }

        @Test
        @DisplayName("a subscription with no period end at all is not refused for its dates")
        void aNullPeriodEndIsNotRefused() {
            // One row in the collection has no currentPeriodEnd. It is @NotNull on the model, so
            // that is a migration artefact — but a gate that threw an NPE on it would take the
            // whole school down rather than refusing one action.
            assertThatCode(() -> check(SubscriptionStatus.ACTIVE, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("every refusal names the school and the subscription number")
        void refusalsAreActionable() {
            assertThatThrownBy(() -> check(SubscriptionStatus.SUSPENDED, future))
                    .hasMessageContaining("Green Valley")
                    .hasMessageContaining("SUB/2026/09/000001");
        }

        @Test
        @DisplayName("a date in a refusal is spelled out, not printed as an instant")
        void datesAreReadable() {
            Instant ended = Instant.parse("2026-08-01T00:00:00Z");

            assertThatThrownBy(() -> check(SubscriptionStatus.ACTIVE, ended))
                    // 00:00Z is 5:30AM in Kolkata on the same day.
                    .hasMessageContaining("Saturday 1 August 2026 5:30AM")
                    .hasMessageNotContaining("2026-08-01T");
        }

        @Test
        @DisplayName("and it is read in the school's zone, not UTC")
        void datesUseTheSchoolZone() {
            // 18:30Z is midnight the NEXT day in Kolkata. UTC would name the 7th.
            Instant ended = Instant.parse("2026-09-07T18:30:00Z");

            assertThatThrownBy(() -> gate.requireUsableSubscription(
                    subscription(SubscriptionStatus.ACTIVE, ended), "Green Valley", KOLKATA))
                    .hasMessageContaining("Tuesday 8 September 2026 12:00AM");
        }

        @Test
        @DisplayName("by id, it reads the school then its current subscription")
        void byIdReadsBoth() {
            when(schools.findById(SCHOOL_ID)).thenReturn(Optional.of(school(SchoolStatus.ACTIVE)));
            when(subscriptions.findBySchoolIdAndCurrentIsTrue(SCHOOL_ID))
                    .thenReturn(Optional.of(subscription(SubscriptionStatus.ACTIVE, future)));

            assertThat(gate.requireUsableSubscription(SCHOOL_ID).getSubscriptionNo())
                    .isEqualTo("SUB/2026/09/000001");
        }

        @Test
        @DisplayName("a school with no subscription is a 404")
        void noSubscriptionIsNotFound() {
            when(schools.findById(SCHOOL_ID)).thenReturn(Optional.of(school(SchoolStatus.ACTIVE)));
            when(subscriptions.findBySchoolIdAndCurrentIsTrue(SCHOOL_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> gate.requireUsableSubscription(SCHOOL_ID))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo("SUBSCRIPTION_NOT_FOUND");
        }
    }

    // ============================================================ gate 3: the academic year

    @Nested
    @DisplayName("the academic year has to be open")
    class TheAcademicYear {

        private LocalDate todayThere() {
            return LocalDate.now(ZoneId.of(KOLKATA));
        }

        @Test
        @DisplayName("running, and today inside it, passes")
        void openYearPasses() {
            AcademicYear open = year(true, todayThere().minusMonths(5), todayThere().plusMonths(5));

            assertThatCode(() -> gate.requireRunningAcademicYear(open, KOLKATA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("not marked as running is refused, even with today inside its dates")
        void notRunningIsRefused() {
            AcademicYear notRunning =
                    year(false, todayThere().minusMonths(5), todayThere().plusMonths(5));

            // The dates alone cannot choose between two overlapping years; the flag is what does.
            assertThatThrownBy(() -> gate.requireRunningAcademicYear(notRunning, KOLKATA))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("is not the year this school is running")
                    .extracting(e -> ((ApiException) e).getCode())
                    .isEqualTo("ACADEMIC_YEAR_NOT_RUNNING");
        }

        @Test
        @DisplayName("marked as running but not started yet is refused, and names the start")
        void notStartedIsRefused() {
            AcademicYear future = year(true, todayThere().plusDays(10), todayThere().plusMonths(12));

            assertThatThrownBy(() -> gate.requireRunningAcademicYear(future, KOLKATA))
                    .hasMessageContaining("has not started yet")
                    .hasMessageContaining("it begins on");
        }

        @Test
        @DisplayName("marked as running but finished is refused, and names the end")
        void finishedIsRefused() {
            AcademicYear over = year(true, todayThere().minusMonths(13), todayThere().minusDays(1));

            assertThatThrownBy(() -> gate.requireRunningAcademicYear(over, KOLKATA))
                    .hasMessageContaining("finished on")
                    .hasMessageContaining("nothing more can be recorded");
        }

        @Test
        @DisplayName("the first day of the year passes — the boundary is inclusive")
        void theFirstDayPasses() {
            AcademicYear startsToday = year(true, todayThere(), todayThere().plusMonths(12));

            assertThatCode(() -> gate.requireRunningAcademicYear(startsToday, KOLKATA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("the last day of the year passes too")
        void theLastDayPasses() {
            AcademicYear endsToday = year(true, todayThere().minusMonths(12), todayThere());

            // A year running to 31 March has to grant ON 31 March, or the school loses a day.
            assertThatCode(() -> gate.requireRunningAcademicYear(endsToday, KOLKATA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("the zone decides which day today is, at every hour of the day")
        void theZoneDecidesToday() {
            // THE FIRST VERSION OF THIS TEST WAS A NO-OP. It compared Asia/Kolkata with
            // Pacific/Midway and skipped itself when the two shared a calendar date — which is
            // most of the day, so it asserted nothing, and reading the date in UTC instead of the
            // school's zone was mutation-tested and changed no result.
            //
            // Kiritimati (UTC+14) and Midway (UTC-11) are 25 hours apart, so they are NEVER on
            // the same calendar date. That makes this deterministic at any hour.
            String ahead = "Pacific/Kiritimati";
            String behind = "Pacific/Midway";
            LocalDate aheadToday = LocalDate.now(ZoneId.of(ahead));
            LocalDate behindToday = LocalDate.now(ZoneId.of(behind));
            assertThat(behindToday).isBefore(aheadToday);

            // A year whose last day is "today" as the western zone sees it.
            AcademicYear endingOnTheEarlierDate =
                    year(true, behindToday.minusMonths(12), behindToday);

            // Open for a school in that zone: today IS its last day, and the bound is inclusive.
            assertThatCode(() ->
                    gate.requireRunningAcademicYear(endingOnTheEarlierDate, behind))
                    .doesNotThrowAnyException();

            // Finished for a school a day ahead. Same document, same instant, different answer —
            // which is the whole reason the gate takes a zone rather than using the server's.
            assertThatThrownBy(() ->
                    gate.requireRunningAcademicYear(endingOnTheEarlierDate, ahead))
                    .hasMessageContaining("finished on");
        }

        @Test
        @DisplayName("an unusable zone falls back to UTC rather than throwing")
        void aBadZoneDoesNotThrow() {
            AcademicYear open = year(true, LocalDate.now(ZoneId.of("UTC")).minusMonths(5),
                    LocalDate.now(ZoneId.of("UTC")).plusMonths(5));

            // A gate that blew up on a bad zone string would refuse everything for that school.
            assertThatCode(() -> gate.requireRunningAcademicYear(open, "Not/AZone"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> gate.requireRunningAcademicYear(open, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a date in a refusal is spelled out")
        void datesAreReadable() {
            AcademicYear over = year(true, LocalDate.of(2020, 4, 1), LocalDate.of(2021, 3, 31));

            assertThatThrownBy(() -> gate.requireRunningAcademicYear(over, KOLKATA))
                    .hasMessageContaining("Wednesday 31 March 2021")
                    .hasMessageNotContaining("2021-03-31");
        }
    }
}
