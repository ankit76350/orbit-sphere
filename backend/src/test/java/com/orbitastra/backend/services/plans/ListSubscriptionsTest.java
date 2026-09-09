package com.orbitastra.backend.services.plans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionSearchRequest;
import com.orbitastra.backend.dto.plans.subscription.response.SubscriptionSummaryResponse;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;
import com.orbitastra.backend.repositories.plans.plandefinition.PlanDefinitionRepository;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;
import com.orbitastra.backend.services.plans.utils.PlatformSubscriptionServiceUtils;

/**
 * #28 — {@code listSubscriptions}, the decisions the service makes before and after the query.
 *
 * <p>These are the things a live call cannot show: <b>how many</b> queries ran, what the
 * repository was actually handed, and which check fired first. The end-to-end behaviour — every
 * filter, every refusal, the shape of the page — is exercised against a real Mongo elsewhere;
 * this is here for the parts that are invisible from outside.
 */
@ExtendWith(MockitoExtension.class)
class ListSubscriptionsTest {

    private static final String SCHOOL_ID = "6a9fea0f7feee1a04ace4b45";
    private static final String PLAN_A = "planA";
    private static final String PLAN_B = "planB";

    @Mock
    private SchoolRepository schools;

    @Mock
    private PlanDefinitionRepository planDefinition;

    @Mock
    private SchoolSubscriptionRepository schoolSubscription;

    /**
     * Mocked although listSubscriptions calls nothing on it. Without it Mockito hands the
     * service a null for this collaborator, and the first helper call added to this endpoint
     * would fail as an NPE rather than as the missing stub it really is.
     */
    @Mock
    private PlatformSubscriptionServiceUtils utils;

    @InjectMocks
    private PlatformSubscriptionService service;

    @Captor
    private ArgumentCaptor<Pageable> pageable;

    @Captor
    private ArgumentCaptor<Iterable<String>> planIdsFetched;

    /** A field rather than {@code forClass}, which cannot express a generic type without a cast. */
    @Captor
    private ArgumentCaptor<Collection<String>> planIdsFiltered;

    /** Every field null — the bare {@code GET} with no parameters at all. */
    private static SubscriptionSearchRequest empty() {
        return new SubscriptionSearchRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private static SubscriptionSearchRequest with(String planCode, Integer page, Integer size,
            String sort, Instant startFrom, Instant startTo, Instant endFrom, Instant endTo) {

        return new SubscriptionSearchRequest(null, null, planCode, null, null, null,
                startFrom, startTo, endFrom, endTo, page, size, sort);
    }

    private static SchoolSubscription subscription(String id, String planId, String number) {
        SchoolSubscription row = new SchoolSubscription();
        row.setId(id);
        row.setSchoolId(SCHOOL_ID);
        row.setSubscriptionNo(number);
        row.setPlanDefinitionDocsId(planId);
        row.setPlanVersion(1);
        row.setStatus(SubscriptionStatus.ACTIVE);
        row.setBillingCycle(BillingCycle.MONTHLY);
        row.setCurrentPeriodStart(Instant.parse("2026-04-01T00:00:00Z"));
        row.setCurrentPeriodEnd(Instant.parse("2026-05-01T00:00:00Z"));
        row.setContractedPrice(new BigDecimal("100.00"));
        row.setCurrencyCode("INR");
        row.setCurrent(false);
        return row;
    }

    private static PlanDefinition plan(String id, String code, String name) {
        PlanDefinition plan = new PlanDefinition();
        plan.setId(id);
        plan.setPlanCode(code);
        plan.setName(name);
        plan.setPlanVersion(1);
        return plan;
    }

    private void schoolExists() {
        when(schools.existsById(SCHOOL_ID)).thenReturn(true);
    }

    private void pageContains(SchoolSubscription... rows) {
        when(schoolSubscription.search(eq(SCHOOL_ID), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(rows),
                        PageRequest.of(0, 20), rows.length));
    }

    @Nested
    @DisplayName("the school")
    class TheSchool {

        @Test
        @DisplayName("a school that does not exist is a 404, and no subscription is read")
        void unknownSchoolIsNotFound() {
            when(schools.existsById(SCHOOL_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.listSubscriptions(SCHOOL_ID, empty()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("No school found with id");

            verifyNoInteractions(schoolSubscription);
        }

        @Test
        @DisplayName("its existence is checked, not the whole document loaded")
        void checksExistenceRatherThanLoading() {
            schoolExists();
            pageContains();

            service.listSubscriptions(SCHOOL_ID, empty());

            verify(schools).existsById(SCHOOL_ID);
            verify(schools, never()).findById(anyString());
        }
    }

    @Nested
    @DisplayName("validation happens before any read")
    class ValidationOrder {

        @Test
        @DisplayName("a bad page is refused without touching the database")
        void badPageCostsNoQuery() {
            assertThatThrownBy(() -> service.listSubscriptions(SCHOOL_ID,
                    with(null, -1, null, null, null, null, null, null)))
                    .isInstanceOf(ApiException.class);

            verifyNoInteractions(schools, schoolSubscription, planDefinition);
        }

        @Test
        @DisplayName("a bad sort field is refused without touching the database")
        void badSortCostsNoQuery() {
            assertThatThrownBy(() -> service.listSubscriptions(SCHOOL_ID,
                    with(null, null, null, "contractedPrice,desc", null, null, null, null)))
                    .isInstanceOf(ApiException.class);

            verifyNoInteractions(schools, schoolSubscription, planDefinition);
        }

        @Test
        @DisplayName("a start window that runs backwards is refused, not answered with zero rows")
        void startWindowBackwards() {
            assertThatThrownBy(() -> service.listSubscriptions(SCHOOL_ID,
                    with(null, null, null, null,
                            Instant.parse("2027-01-01T00:00:00Z"),
                            Instant.parse("2026-01-01T00:00:00Z"), null, null)))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("startDateFrom")
                    .hasMessageContaining("must not be after startDateTo");

            verifyNoInteractions(schools, schoolSubscription);
        }

        @Test
        @DisplayName("an end window that runs backwards is refused too")
        void endWindowBackwards() {
            assertThatThrownBy(() -> service.listSubscriptions(SCHOOL_ID,
                    with(null, null, null, null, null, null,
                            Instant.parse("2027-01-01T00:00:00Z"),
                            Instant.parse("2026-01-01T00:00:00Z"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("endDateFrom");
        }

        @Test
        @DisplayName("one end of a window on its own is not a range and is allowed")
        void oneEndedWindowIsFine() {
            schoolExists();
            pageContains();

            service.listSubscriptions(SCHOOL_ID, with(null, null, null, null,
                    Instant.parse("2027-01-01T00:00:00Z"), null, null, null));

            verify(schoolSubscription).search(eq(SCHOOL_ID), any(), any(), any());
        }

        @Test
        @DisplayName("the two ends being equal is a valid window, not backwards")
        void equalEndsAreValid() {
            schoolExists();
            pageContains();
            Instant same = Instant.parse("2027-01-01T00:00:00Z");

            service.listSubscriptions(SCHOOL_ID,
                    with(null, null, null, null, same, same, null, null));

            verify(schoolSubscription).search(eq(SCHOOL_ID), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("the planCode filter is resolved to plan ids")
    class PlanCodeResolution {

        @Test
        @DisplayName("no planCode means NULL is passed down — no plan filter at all")
        void noPlanCodeMeansNoFilter() {
            schoolExists();
            pageContains();

            service.listSubscriptions(SCHOOL_ID, empty());

            verify(schoolSubscription).search(eq(SCHOOL_ID), any(), planIdsFiltered.capture(), any());
            assertThat(planIdsFiltered.getValue()).isNull();
            verify(planDefinition, never()).findByPlanCodeOrderByPlanVersionDesc(anyString());
        }

        @Test
        @DisplayName("a code resolves to the ids of its versions")
        void codeResolvesToVersionIds() {
            schoolExists();
            pageContains();
            when(planDefinition.findByPlanCodeOrderByPlanVersionDesc("PREMIUM"))
                    .thenReturn(List.of(plan(PLAN_A, "PREMIUM", "Premium"),
                            plan(PLAN_B, "PREMIUM", "Premium")));

            service.listSubscriptions(SCHOOL_ID,
                    with("PREMIUM", null, null, null, null, null, null, null));

            verify(schoolSubscription).search(eq(SCHOOL_ID), any(), planIdsFiltered.capture(), any());
            assertThat(planIdsFiltered.getValue()).containsExactlyInAnyOrder(PLAN_A, PLAN_B);
        }

        @Test
        @DisplayName("the code is normalised, so premium-plus finds PREMIUM_PLUS")
        void codeIsNormalised() {
            schoolExists();
            pageContains();
            when(planDefinition.findByPlanCodeOrderByPlanVersionDesc("PREMIUM_PLUS"))
                    .thenReturn(List.of());

            service.listSubscriptions(SCHOOL_ID,
                    with("  premium-plus ", null, null, null, null, null, null, null));

            verify(planDefinition).findByPlanCodeOrderByPlanVersionDesc("PREMIUM_PLUS");
        }

        /**
         * A code that matches nothing must pass an EMPTY collection down, not null: null means
         * "no plan filter" and would return the school's whole history for a filter that should
         * have matched nothing at all.
         */
        @Test
        @DisplayName("a code matching no plan passes an EMPTY set, which matches nothing")
        void unmatchedCodePassesEmptyNotNull() {
            schoolExists();
            pageContains();
            when(planDefinition.findByPlanCodeOrderByPlanVersionDesc("NOPE"))
                    .thenReturn(List.of());

            service.listSubscriptions(SCHOOL_ID,
                    with("NOPE", null, null, null, null, null, null, null));

            verify(schoolSubscription).search(eq(SCHOOL_ID), any(), planIdsFiltered.capture(), any());
            assertThat(planIdsFiltered.getValue()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("a blank planCode is no filter, not a code of nothing")
        void blankCodeIsNoFilter() {
            schoolExists();
            pageContains();

            service.listSubscriptions(SCHOOL_ID,
                    with("   ", null, null, null, null, null, null, null));

            verify(schoolSubscription).search(eq(SCHOOL_ID), any(), planIdsFiltered.capture(), any());
            assertThat(planIdsFiltered.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("the plans behind a page are fetched once")
    class NoNPlusOne {

        /**
         * THE POINT OF THIS CLASS. Each row needs its plan's code and name, and the obvious way
         * to get them — a lookup per row — makes a twenty-row page cost twenty-one queries.
         */
        @Test
        @DisplayName("five rows on two plans cost ONE plan query, not five")
        void oneQueryForTheWholePage() {
            schoolExists();
            pageContains(
                    subscription("s1", PLAN_A, "SUB/1"),
                    subscription("s2", PLAN_A, "SUB/2"),
                    subscription("s3", PLAN_B, "SUB/3"),
                    subscription("s4", PLAN_A, "SUB/4"),
                    subscription("s5", PLAN_B, "SUB/5"));
            when(planDefinition.findAllById(any()))
                    .thenReturn(List.of(plan(PLAN_A, "A", "Plan A"), plan(PLAN_B, "B", "Plan B")));

            service.listSubscriptions(SCHOOL_ID, empty());

            verify(planDefinition, times(1)).findAllById(planIdsFetched.capture());
            assertThat(planIdsFetched.getValue()).containsExactlyInAnyOrder(PLAN_A, PLAN_B);
        }

        @Test
        @DisplayName("and it asks for DISTINCT ids, so repeated renewals cost nothing extra")
        void asksForDistinctIds() {
            schoolExists();
            pageContains(
                    subscription("s1", PLAN_A, "SUB/1"),
                    subscription("s2", PLAN_A, "SUB/2"),
                    subscription("s3", PLAN_A, "SUB/3"));
            when(planDefinition.findAllById(any()))
                    .thenReturn(List.of(plan(PLAN_A, "A", "Plan A")));

            service.listSubscriptions(SCHOOL_ID, empty());

            verify(planDefinition).findAllById(planIdsFetched.capture());
            assertThat(planIdsFetched.getValue()).containsExactly(PLAN_A);
        }

        @Test
        @DisplayName("an empty page fetches no plans at all")
        void emptyPageFetchesNothing() {
            schoolExists();
            pageContains();

            PageResponse<SubscriptionSummaryResponse> result =
                    service.listSubscriptions(SCHOOL_ID, empty());

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(planDefinition, never()).findAllById(any());
        }

        @Test
        @DisplayName("each row is matched to its own plan, not to the first one found")
        void rowsAreMatchedToTheirOwnPlan() {
            schoolExists();
            pageContains(
                    subscription("s1", PLAN_A, "SUB/1"),
                    subscription("s2", PLAN_B, "SUB/2"));
            when(planDefinition.findAllById(any())).thenReturn(List.of(
                    plan(PLAN_A, "AAA", "Plan A"), plan(PLAN_B, "BBB", "Plan B")));

            List<SubscriptionSummaryResponse> rows =
                    service.listSubscriptions(SCHOOL_ID, empty()).content();

            assertThat(rows).extracting(SubscriptionSummaryResponse::planCode)
                    .containsExactly("AAA", "BBB");
        }

        /**
         * A history is exactly where a since-deleted plan turns up, and one unreadable row is
         * worth more than a 500 for the other nineteen.
         */
        @Test
        @DisplayName("a row whose plan has gone keeps its own fields and nulls the plan's")
        void missingPlanDoesNotFailThePage() {
            schoolExists();
            pageContains(subscription("s1", "goneForever", "SUB/1"));
            when(planDefinition.findAllById(any())).thenReturn(List.of());

            List<SubscriptionSummaryResponse> rows =
                    service.listSubscriptions(SCHOOL_ID, empty()).content();

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).planCode()).isNull();
            assertThat(rows.get(0).planName()).isNull();
            assertThat(rows.get(0).subscriptionNo()).isEqualTo("SUB/1");
            assertThat(rows.get(0).status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("the response")
    class TheResponse {

        @Test
        @DisplayName("the default order is newest period first, tiebroken on subscriptionNo")
        void defaultOrderIsStable() {
            schoolExists();
            pageContains();

            service.listSubscriptions(SCHOOL_ID, empty());

            verify(schoolSubscription).search(eq(SCHOOL_ID), any(), any(), pageable.capture());
            assertThat(pageable.getValue().getSort().stream().map(Sort.Order::getProperty))
                    .containsExactly("currentPeriodStart", "subscriptionNo");
            assertThat(pageable.getValue().getSort().getOrderFor("subscriptionNo").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("periodEnded is computed rather than read off the status")
        void periodEndedIsComputed() {
            schoolExists();
            SchoolSubscription lapsed = subscription("s1", PLAN_A, "SUB/1");
            lapsed.setStatus(SubscriptionStatus.ACTIVE);
            lapsed.setCurrentPeriodEnd(Instant.parse("2020-01-01T00:00:00Z"));
            pageContains(lapsed);
            when(planDefinition.findAllById(any())).thenReturn(List.of(plan(PLAN_A, "A", "A")));

            SubscriptionSummaryResponse row =
                    service.listSubscriptions(SCHOOL_ID, empty()).content().get(0);

            // Nothing marks a lapsed subscription EXPIRED yet, so a row can read ACTIVE with a
            // period that finished years ago. The flag is the only thing that says so.
            assertThat(row.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(row.periodEnded()).isTrue();
        }

        @Test
        @DisplayName("a period still running is not marked ended")
        void runningPeriodIsNotEnded() {
            schoolExists();
            SchoolSubscription live = subscription("s1", PLAN_A, "SUB/1");
            live.setCurrentPeriodEnd(Instant.now().plusSeconds(86_400));
            pageContains(live);
            when(planDefinition.findAllById(any())).thenReturn(List.of(plan(PLAN_A, "A", "A")));

            assertThat(service.listSubscriptions(SCHOOL_ID, empty()).content().get(0)
                    .periodEnded()).isFalse();
        }

        @Test
        @DisplayName("no internal plan document id reaches the caller")
        void internalIdIsNotExposed() {
            assertThat(SubscriptionSummaryResponse.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("planDefinitionDocsId", "billingCustomerReference",
                            "schoolId");
        }
    }
}
