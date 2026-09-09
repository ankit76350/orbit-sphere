package com.orbitastra.backend.services.plans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.plans.subscription.request.PlatformSubscriptionSearchRequest;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.core.enums.SchoolStatus;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;
import com.orbitastra.backend.repositories.plans.plandefinition.PlanDefinitionRepository;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;
import com.orbitastra.backend.services.plans.helper.PlansHelper;

/**
 * What #30 does around the query: how many reads, in what order, and what it hands over.
 *
 * <p>No database. These are the things an end-to-end test cannot see — that the validation runs
 * before anything is read, that a page of twenty rows from twenty different schools costs
 * <b>one</b> school lookup and not twenty, and that a school or plan the row points at but which
 * no longer exists leaves that row's fields null rather than failing the page.
 *
 * <p>It also pins the one thing that separates this endpoint from #28: <b>no query it runs is
 * scoped to a school.</b>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListAllSubscriptionsTest {

    @Mock private SchoolRepository schools;
    @Mock private PlanDefinitionRepository planDefinition;
    @Mock private SchoolSubscriptionRepository schoolSubscription;

    /**
     * A REAL helper, spied rather than mocked. It is stateless with no dependencies of its own,
     * and the normalisation case below is meant to prove that {@code premium-plus} really does
     * reach the repository as {@code PREMIUM_PLUS} — against a stub it would only prove the stub.
     */
    @Spy private PlansHelper helper = new PlansHelper();

    /** Constructor arguments the service needs; listAllSubscriptions touches none of them. */
    @Mock private com.orbitastra.backend.repositories.plans.subscriptionhistory.SubscriptionHistoryRepository history;
    @Mock private com.orbitastra.backend.services.institution.NumberSequenceService numberSequences;
    @Mock private com.orbitastra.backend.services.core.SchoolPlatformService schoolPlatform;

    /**
     * REAL utils, not a mock — {@code planFrom} is code this test checks the behaviour of, and
     * against a stub it would only be proving the stub. Same reasoning as the spied helper above.
     */
    private com.orbitastra.backend.services.plans.utils.PlatformSubscriptionServiceUtils utils;

    private PlatformSubscriptionService service;

    @BeforeEach
    void wire() {
        utils = new com.orbitastra.backend.services.plans.utils.PlatformSubscriptionServiceUtils(
                schools, planDefinition, schoolSubscription, helper, schoolPlatform);
        service = new PlatformSubscriptionService(schools, planDefinition, schoolSubscription,
                history, numberSequences, helper, utils, schoolPlatform);
    }

    private static PlatformSubscriptionSearchRequest request() {
        return new PlatformSubscriptionSearchRequest(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private SchoolSubscription row(String id, String schoolId, String planId) {
        SchoolSubscription s = new SchoolSubscription();
        s.setId(id);
        s.setSchoolId(schoolId);
        s.setSubscriptionNo("SUB/2026/09/000001");
        s.setPlanDefinitionDocsId(planId);
        s.setPlanVersion(1);
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setBillingCycle(BillingCycle.MONTHLY);
        s.setCurrentPeriodStart(Instant.parse("2026-09-01T00:00:00Z"));
        s.setCurrentPeriodEnd(Instant.parse("2026-10-01T00:00:00Z"));
        s.setContractedPrice(new BigDecimal("100.00"));
        s.setCurrencyCode("INR");
        s.setAutoRenew(true);
        s.setCurrent(true);
        return s;
    }

    private School school(String id, String name) {
        School s = new School();
        s.setId(id);
        s.setSchoolName(name);
        s.setSubdomain(name.toLowerCase());
        s.setStatus(SchoolStatus.ACTIVE);
        return s;
    }

    private PlanDefinition plan(String id, String code) {
        PlanDefinition p = new PlanDefinition();
        p.setId(id);
        p.setPlanCode(code);
        p.setPlanVersion(1);
        p.setName("Plan " + code);
        return p;
    }

    private void givenRows(List<SchoolSubscription> rows) {
        when(schoolSubscription.searchAcrossSchools(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(rows, PageRequest.of(0, 20), rows.size()));
    }

    // -------------------------------------------------------- no tenant, anywhere

    @Test
    @DisplayName("it uses the cross-school query, never the school-scoped one")
    void itUsesTheCrossSchoolQuery() {
        givenRows(List.of());

        service.listAllSubscriptions(request());

        verify(schoolSubscription).searchAcrossSchools(any(), any(), any(Pageable.class));
        // The school-scoped search takes a schoolId first. Reaching for it here would mean this
        // endpoint had a tenant it should not have.
        verify(schoolSubscription, never()).search(anyString(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("no read it makes is scoped to one school")
    void nothingIsScopedToASchool() {
        givenRows(List.of(row("s1", "schoolA", "planA")));
        when(schools.findAllById(any())).thenReturn(List.of(school("schoolA", "A")));
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planA", "A")));

        service.listAllSubscriptions(request());

        // Schools are read by the ids ON the page, not by a tenant the caller supplied.
        verify(schools, never()).findById(anyString());
        verify(schools, never()).existsById(anyString());
    }

    // -------------------------------------------------------- validation first

    @Test
    @DisplayName("a bad page is refused before anything is read")
    void badPageIsRefusedFirst() {
        PlatformSubscriptionSearchRequest bad = new PlatformSubscriptionSearchRequest(
                null, null, null, null, null, null, null, null, null, null, -1, null, null);

        assertThatThrownBy(() -> service.listAllSubscriptions(bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("page cannot be negative");

        verifyNoInteractions(schools, schoolSubscription, planDefinition);
    }

    @Test
    @DisplayName("a sort field off the allow-list is refused before anything is read")
    void badSortIsRefusedFirst() {
        PlatformSubscriptionSearchRequest bad = new PlatformSubscriptionSearchRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                "schoolName,asc");

        assertThatThrownBy(() -> service.listAllSubscriptions(bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be sorted on");

        verifyNoInteractions(schools, schoolSubscription, planDefinition);
    }

    @Test
    @DisplayName("subscriptionNo is NOT sortable here, unlike #28")
    void subscriptionNoIsNotSortable() {
        PlatformSubscriptionSearchRequest bad = new PlatformSubscriptionSearchRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                "subscriptionNo,asc");

        // It is unique only within a school, so across the platform it orders nothing.
        assertThatThrownBy(() -> service.listAllSubscriptions(bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("subscriptionNo");
    }

    @Test
    @DisplayName("a backwards start window is refused, with its dates spelled out")
    void backwardsStartWindowIsRefused() {
        PlatformSubscriptionSearchRequest bad = new PlatformSubscriptionSearchRequest(
                null, null, null, null, null, null,
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                null, null, null, null, null);

        assertThatThrownBy(() -> service.listAllSubscriptions(bad))
                .hasMessageContaining("startDateFrom")
                .hasMessageContaining("Friday 1 January 2027")
                .hasMessageNotContaining("2027-01-01T");

        verifyNoInteractions(schoolSubscription);
    }

    @Test
    @DisplayName("a backwards end window is refused too, naming that pair")
    void backwardsEndWindowIsRefused() {
        PlatformSubscriptionSearchRequest bad = new PlatformSubscriptionSearchRequest(
                null, null, null, null, null, null, null, null,
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                null, null, null);

        assertThatThrownBy(() -> service.listAllSubscriptions(bad))
                .hasMessageContaining("endDateFrom")
                .hasMessageContaining("endDateTo");
    }

    // -------------------------------------------------------- the order

    @Test
    @DisplayName("the default order is soonest to end first, then the row id")
    void defaultOrderEndsInAUniqueKey() {
        givenRows(List.of());

        service.listAllSubscriptions(request());

        ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolSubscription).searchAcrossSchools(any(), any(), paging.capture());
        List<Sort.Order> orders = paging.getValue().getSort().toList();

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo("currentPeriodEnd");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        // The id, NOT subscriptionNo: that is unique only within a school.
        assertThat(orders.get(1).getProperty()).isEqualTo("id");
    }

    @Test
    @DisplayName("a caller's sort goes first and keeps the tiebreaker underneath")
    void callerSortKeepsTheTiebreaker() {
        givenRows(List.of());
        PlatformSubscriptionSearchRequest sorted = new PlatformSubscriptionSearchRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                "contractedPrice,desc");

        service.listAllSubscriptions(sorted);

        ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolSubscription).searchAcrossSchools(any(), any(), paging.capture());

        assertThat(paging.getValue().getSort().toList().stream().map(Sort.Order::getProperty))
                .containsExactly("contractedPrice", "currentPeriodEnd", "id");
    }

    @Test
    @DisplayName("sorting on the default key does not emit it twice")
    void aNamedDefaultKeyIsNotEmittedTwice() {
        givenRows(List.of());
        PlatformSubscriptionSearchRequest sorted = new PlatformSubscriptionSearchRequest(
                null, null, null, null, null, null, null, null, null, null, null, null,
                "currentPeriodEnd,desc");

        service.listAllSubscriptions(sorted);

        ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolSubscription).searchAcrossSchools(any(), any(), paging.capture());
        List<Sort.Order> orders = paging.getValue().getSort().toList();

        assertThat(orders.stream().filter(o -> o.getProperty().equals("currentPeriodEnd")).count())
                .isEqualTo(1);
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders).hasSize(2);
    }

    // -------------------------------------------------------- planCode resolution

    @Test
    @DisplayName("a planCode is normalised before it is resolved")
    void planCodeIsNormalised() {
        givenRows(List.of());
        when(planDefinition.findByPlanCodeOrderByPlanVersionDesc(anyString()))
                .thenReturn(List.of(plan("planA", "PREMIUM_PLUS")));
        PlatformSubscriptionSearchRequest filtered = new PlatformSubscriptionSearchRequest(
                null, null, "premium-plus", null, null, null, null, null, null, null,
                null, null, null);

        service.listAllSubscriptions(filtered);

        verify(planDefinition).findByPlanCodeOrderByPlanVersionDesc("PREMIUM_PLUS");
    }

    @Test
    @DisplayName("no planCode means a NULL plan filter, which is no filter at all")
    void noPlanCodeMeansNoFilter() {
        givenRows(List.of());

        service.listAllSubscriptions(request());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> ids = ArgumentCaptor.forClass(Set.class);
        verify(schoolSubscription).searchAcrossSchools(any(), ids.capture(), any(Pageable.class));
        assertThat(ids.getValue()).isNull();
        verify(planDefinition, never()).findByPlanCodeOrderByPlanVersionDesc(anyString());
    }

    @Test
    @DisplayName("a planCode matching no plan means an EMPTY set, which matches nothing")
    void unmatchedPlanCodeMeansEmptySet() {
        givenRows(List.of());
        when(planDefinition.findByPlanCodeOrderByPlanVersionDesc(anyString()))
                .thenReturn(List.of());
        PlatformSubscriptionSearchRequest filtered = new PlatformSubscriptionSearchRequest(
                null, null, "NO_SUCH", null, null, null, null, null, null, null, null, null, null);

        service.listAllSubscriptions(filtered);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> ids = ArgumentCaptor.forClass(Set.class);
        verify(schoolSubscription).searchAcrossSchools(any(), ids.capture(), any(Pageable.class));

        // EMPTY, not null: an unmatched filter must match nothing rather than everything.
        assertThat(ids.getValue()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("a blank planCode is treated as absent")
    void blankPlanCodeIsAbsent() {
        givenRows(List.of());
        PlatformSubscriptionSearchRequest filtered = new PlatformSubscriptionSearchRequest(
                null, null, "   ", null, null, null, null, null, null, null, null, null, null);

        service.listAllSubscriptions(filtered);

        verify(planDefinition, never()).findByPlanCodeOrderByPlanVersionDesc(anyString());
    }

    // -------------------------------------------------------- N+1, twice over

    @Test
    @DisplayName("twenty rows from twenty schools cost ONE school lookup, not twenty")
    void oneQueryForEverySchoolOnThePage() {
        List<SchoolSubscription> rows = new ArrayList<>();
        List<School> found = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            rows.add(row("s" + i, "school" + i, "planA"));
            found.add(school("school" + i, "S" + i));
        }
        givenRows(rows);
        when(schools.findAllById(any())).thenReturn(found);
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planA", "A")));

        service.listAllSubscriptions(request());

        verify(schools, times(1)).findAllById(any());
        verify(schools, never()).findById(anyString());
    }

    @Test
    @DisplayName("and ONE plan lookup, over distinct ids")
    void oneQueryForEveryPlanOnThePage() {
        givenRows(List.of(row("s1", "schoolA", "planA"), row("s2", "schoolB", "planA"),
                row("s3", "schoolC", "planB")));
        when(schools.findAllById(any())).thenReturn(List.of(
                school("schoolA", "A"), school("schoolB", "B"), school("schoolC", "C")));
        when(planDefinition.findAllById(any()))
                .thenReturn(List.of(plan("planA", "A"), plan("planB", "B")));

        service.listAllSubscriptions(request());

        verify(planDefinition, times(1)).findAllById(any());
        verify(planDefinition, never()).findById(anyString());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> ids = ArgumentCaptor.forClass(Set.class);
        verify(planDefinition).findAllById(ids.capture());
        // planA is on two rows and is asked for once.
        assertThat(ids.getValue()).containsExactlyInAnyOrder("planA", "planB");
    }

    @Test
    @DisplayName("the school ids asked for are the distinct ones on the page")
    void schoolIdsAreDistinct() {
        givenRows(List.of(row("s1", "schoolA", "planA"), row("s2", "schoolA", "planA"),
                row("s3", "schoolB", "planA")));
        when(schools.findAllById(any())).thenReturn(List.of(
                school("schoolA", "A"), school("schoolB", "B")));
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planA", "A")));

        service.listAllSubscriptions(request());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> ids = ArgumentCaptor.forClass(Set.class);
        verify(schools).findAllById(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder("schoolA", "schoolB");
    }

    @Test
    @DisplayName("an empty page queries neither schools nor plans")
    void anEmptyPageQueriesNothingElse() {
        givenRows(List.of());

        service.listAllSubscriptions(request());

        verify(schools, never()).findAllById(any());
        verify(planDefinition, never()).findAllById(any());
    }

    // -------------------------------------------------------- what comes back

    @Test
    @DisplayName("a row names its school and its plan")
    void aRowNamesItsSchoolAndPlan() {
        givenRows(List.of(row("s1", "schoolA", "planA")));
        when(schools.findAllById(any())).thenReturn(List.of(school("schoolA", "Alpha")));
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planA", "PREMIUM")));

        var out = service.listAllSubscriptions(request()).content().get(0);

        assertThat(out.schoolId()).isEqualTo("schoolA");
        assertThat(out.schoolName()).isEqualTo("Alpha");
        assertThat(out.subdomain()).isEqualTo("alpha");
        assertThat(out.schoolStatus()).isEqualTo(SchoolStatus.ACTIVE);
        assertThat(out.planCode()).isEqualTo("PREMIUM");
        assertThat(out.planName()).isEqualTo("Plan PREMIUM");
    }

    @Test
    @DisplayName("a school that has gone leaves its fields null but keeps the id")
    void aMissingSchoolLeavesItsFieldsNull() {
        givenRows(List.of(row("s1", "goneForever", "planA")));
        when(schools.findAllById(any())).thenReturn(List.of());
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planA", "A")));

        var out = service.listAllSubscriptions(request()).content().get(0);

        assertThat(out.schoolName()).isNull();
        assertThat(out.subdomain()).isNull();
        assertThat(out.schoolStatus()).isNull();
        // Still traceable, which is what somebody investigating an orphan needs.
        assertThat(out.schoolId()).isEqualTo("goneForever");
        assertThat(out.planCode()).isEqualTo("A");
    }

    @Test
    @DisplayName("a plan that has gone leaves its fields null and does not fail the page")
    void aMissingPlanLeavesItsFieldsNull() {
        givenRows(List.of(row("s1", "schoolA", "goneForever")));
        when(schools.findAllById(any())).thenReturn(List.of(school("schoolA", "Alpha")));
        when(planDefinition.findAllById(any())).thenReturn(List.of());

        var out = service.listAllSubscriptions(request()).content().get(0);

        assertThat(out.planCode()).isNull();
        assertThat(out.planName()).isNull();
        assertThat(out.schoolName()).isEqualTo("Alpha");
        // The version is on the subscription itself, so it survives.
        assertThat(out.planVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("a row with no plan link at all does not throw on the empty map")
    void aRowWithNoPlanLinkDoesNotThrow() {
        // THE BUG THIS GUARDS: Map.of().get(null) rejects the key rather than answering null, so
        // the page that needed no plan lookup was the one that failed. #28 and #29 had it too.
        givenRows(List.of(row("s1", "schoolA", null)));
        when(schools.findAllById(any())).thenReturn(List.of(school("schoolA", "Alpha")));

        var page = service.listAllSubscriptions(request());

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).planCode()).isNull();
    }

    @Test
    @DisplayName("periodEnded is worked out once for the whole page")
    void periodEndedIsOneDecisionPerPage() {
        SchoolSubscription lapsed = row("s1", "schoolA", "planA");
        lapsed.setCurrentPeriodEnd(Instant.parse("2020-01-01T00:00:00Z"));
        SchoolSubscription live = row("s2", "schoolB", "planA");
        live.setCurrentPeriodEnd(Instant.parse("2099-01-01T00:00:00Z"));
        givenRows(List.of(lapsed, live));
        when(schools.findAllById(any())).thenReturn(List.of(
                school("schoolA", "A"), school("schoolB", "B")));
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planA", "A")));

        var rows = service.listAllSubscriptions(request()).content();

        assertThat(rows.get(0).periodEnded()).isTrue();
        assertThat(rows.get(1).periodEnded()).isFalse();
    }

    @Test
    @DisplayName("an empty platform is an empty page, not an error")
    void anEmptyPlatformIsAnEmptyPage() {
        givenRows(List.of());

        var page = service.listAllSubscriptions(request());

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("nothing about listing writes")
    void listingNeverWrites() {
        givenRows(List.of(row("s1", "schoolA", "planA")));
        when(schools.findAllById(any())).thenReturn(List.of(school("schoolA", "A")));
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planA", "A")));

        service.listAllSubscriptions(request());

        verify(schoolSubscription, never()).save(any());
        verify(schools, never()).save(any());
        verify(planDefinition, never()).save(any());
    }
}
