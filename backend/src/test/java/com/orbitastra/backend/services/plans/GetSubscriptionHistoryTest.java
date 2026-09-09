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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionHistorySearchRequest;
import com.orbitastra.backend.dto.plans.subscription.response.SubscriptionHistoryEntryResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.plans.PlanDefinition;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.SubscriptionHistory;
import com.orbitastra.backend.models.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;
import com.orbitastra.backend.repositories.core.school.SchoolRepository;
import com.orbitastra.backend.repositories.plans.plandefinition.PlanDefinitionRepository;
import com.orbitastra.backend.repositories.plans.schoolsubscription.SchoolSubscriptionRepository;
import com.orbitastra.backend.repositories.plans.subscriptionhistory.SubscriptionHistoryRepository;
import com.orbitastra.backend.services.core.SchoolPlatformService;
import com.orbitastra.backend.services.institution.NumberSequenceService;
import com.orbitastra.backend.services.plans.helper.PlansHelper;
import com.orbitastra.backend.services.plans.utils.PlatformSubscriptionServiceUtils;

/**
 * What #29 does around the query: how many reads it makes, in what order, and what it hands over.
 *
 * <p>No database. These are the things an end-to-end test cannot see — that the validation runs
 * before anything is read, that a page of twenty plan changes costs <b>one</b> plan lookup and not
 * forty, and that a plan the row points at but which no longer exists leaves the row's plan
 * fields null instead of failing the page.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetSubscriptionHistoryTest {

    private static final String SCHOOL = "6aa15d9dc3f7d0033333333a";
    private static final String SUB_ID = "6aa1a44dc3f7d0011223344b";
    private static final String SUB_NO = "SUB/2026/09/000001";

    @Mock private SchoolRepository schools;
    @Mock private PlanDefinitionRepository planDefinition;
    @Mock private SchoolSubscriptionRepository schoolSubscription;
    @Mock private SubscriptionHistoryRepository history;
    @Mock private NumberSequenceService numberSequences;
    @Mock private SchoolPlatformService schoolPlatform;
    @Spy  private PlansHelper helper = new PlansHelper();

    // The real utils, so findSchoolSubscription and planFrom actually run rather than being
    // stubbed into agreeing with the test.
    private PlatformSubscriptionServiceUtils utils;
    private PlatformSubscriptionService service;

    private School school() {
        School s = new School();
        s.setId(SCHOOL);
        s.setSchoolName("Test School");
        s.setDefaultTimeZone("Asia/Kolkata");
        return s;
    }

    private SchoolSubscription subscription() {
        SchoolSubscription s = new SchoolSubscription();
        s.setId(SUB_ID);
        s.setSchoolId(SCHOOL);
        s.setSubscriptionNo(SUB_NO);
        s.setCurrent(true);
        return s;
    }

    private SubscriptionHistory row(String id, String previousPlan, String newPlan) {
        SubscriptionHistory h = new SubscriptionHistory();
        h.setId(id);
        h.setSchoolId(SCHOOL);
        h.setSchoolSubscriptionDocsId(SUB_ID);
        h.setEventType(SubscriptionEventType.PLAN_CHANGED);
        h.setNewStatus(SubscriptionStatus.ACTIVE);
        h.setPreviousPlanDefinitionDocsId(previousPlan);
        h.setNewPlanDefinitionDocsId(newPlan);
        h.setSource("ADMIN_PORTAL");
        h.setEffectiveAt(Instant.parse("2026-09-10T00:00:00Z"));
        h.setCreatedAt(Instant.parse("2026-09-09T00:00:00Z"));
        return h;
    }

    private PlanDefinition plan(String id, String code) {
        PlanDefinition p = new PlanDefinition();
        p.setId(id);
        p.setPlanCode(code);
        p.setPlanVersion(1);
        p.setName("Plan " + code);
        return p;
    }

    private static SubscriptionHistorySearchRequest request() {
        return new SubscriptionHistorySearchRequest(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private void wire() {
        utils = new PlatformSubscriptionServiceUtils(schools, planDefinition,
                schoolSubscription, helper, schoolPlatform);
        service = new PlatformSubscriptionService(schools, planDefinition, schoolSubscription,
                history, numberSequences, helper, utils, schoolPlatform);
    }

    private void givenTrail(List<SubscriptionHistory> rows) {
        when(schools.findById(SCHOOL)).thenReturn(Optional.of(school()));
        when(schoolSubscription.findBySchoolIdAndCurrentIsTrue(SCHOOL))
                .thenReturn(Optional.of(subscription()));
        when(history.search(anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(rows, PageRequest.of(0, 20), rows.size()));
    }

    // -------------------------------------------------------- validation runs first

    @Test
    @DisplayName("a bad page is refused before the school is read")
    void badPageIsRefusedBeforeAnyRead() {
        wire();
        SubscriptionHistorySearchRequest bad = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                -1, null, null);

        assertThatThrownBy(() -> service.getSubscriptionHistory(SCHOOL, "current", bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("page cannot be negative");

        verifyNoInteractions(schools, history);
    }

    @Test
    @DisplayName("a bad sort field is refused before the school is read")
    void badSortIsRefusedBeforeAnyRead() {
        wire();
        SubscriptionHistorySearchRequest bad = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, "reason,desc");

        assertThatThrownBy(() -> service.getSubscriptionHistory(SCHOOL, "current", bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be sorted on");

        verifyNoInteractions(schools, history);
    }

    @Test
    @DisplayName("a backwards effective window is refused before the school is read")
    void backwardsWindowIsRefusedBeforeAnyRead() {
        wire();
        SubscriptionHistorySearchRequest bad = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null,
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                null, null, null, null, null);

        assertThatThrownBy(() -> service.getSubscriptionHistory(SCHOOL, "current", bad))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not be after");

        verifyNoInteractions(schools, history);
    }

    @Test
    @DisplayName("the backwards-window refusal spells its dates out")
    void refusalSpellsTheDatesOut() {
        wire();
        SubscriptionHistorySearchRequest bad = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null,
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                null, null, null, null, null);

        assertThatThrownBy(() -> service.getSubscriptionHistory(SCHOOL, "current", bad))
                .hasMessageContaining("Friday 1 January 2027")
                .hasMessageNotContaining("2027-01-01T");
    }

    @Test
    @DisplayName("a backwards recorded window is refused too, naming that pair")
    void backwardsRecordedWindowIsRefused() {
        wire();
        SubscriptionHistorySearchRequest bad = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null, null, null,
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                null, null, null);

        assertThatThrownBy(() -> service.getSubscriptionHistory(SCHOOL, "current", bad))
                .hasMessageContaining("recordedFrom")
                .hasMessageContaining("recordedTo");
    }

    // -------------------------------------------------------- not found

    @Test
    @DisplayName("a school that does not exist is a 404 and reads no history")
    void missingSchoolIsNotFound() {
        wire();
        when(schools.findById(SCHOOL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubscriptionHistory(SCHOOL, "current", request()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No school found");

        verifyNoInteractions(history);
    }

    @Test
    @DisplayName("a school with no current subscription is a 404 and reads no history")
    void missingSubscriptionIsNotFound() {
        wire();
        when(schools.findById(SCHOOL)).thenReturn(Optional.of(school()));
        when(schoolSubscription.findBySchoolIdAndCurrentIsTrue(SCHOOL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubscriptionHistory(SCHOOL, "current", request()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("has no subscription yet");

        verifyNoInteractions(history);
    }

    @Test
    @DisplayName("an id-shaped path segment is looked up by id, scoped to the school")
    void anIdIsLookedUpById() {
        wire();
        when(schools.findById(SCHOOL)).thenReturn(Optional.of(school()));
        when(schoolSubscription.findBySchoolIdAndId(SCHOOL, SUB_ID))
                .thenReturn(Optional.of(subscription()));
        when(history.search(anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.getSubscriptionHistory(SCHOOL, SUB_ID, request());

        // The school id is in the lookup even though the document id is unique on its own.
        verify(schoolSubscription).findBySchoolIdAndId(SCHOOL, SUB_ID);
        verify(schoolSubscription, never()).findBySchoolIdAndSubscriptionNo(anyString(), anyString());
    }

    @Test
    @DisplayName("anything else is looked up as a subscription number")
    void anythingElseIsLookedUpAsANumber() {
        wire();
        when(schools.findById(SCHOOL)).thenReturn(Optional.of(school()));
        when(schoolSubscription.findBySchoolIdAndSubscriptionNo(SCHOOL, "SUB-123"))
                .thenReturn(Optional.of(subscription()));
        when(history.search(anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.getSubscriptionHistory(SCHOOL, "SUB-123", request());

        verify(schoolSubscription).findBySchoolIdAndSubscriptionNo(SCHOOL, "SUB-123");
        verify(schoolSubscription, never()).findBySchoolIdAndId(anyString(), anyString());
    }

    // -------------------------------------------------------- what the query is handed

    @Test
    @DisplayName("the query is scoped to the school AND the subscription's own id")
    void queryIsScopedToBoth() {
        wire();
        givenTrail(List.of());

        service.getSubscriptionHistory(SCHOOL, "current", request());

        verify(history).search(eq(SCHOOL), eq(SUB_ID), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("the default order is newest effective, then newest written, then the row id")
    void defaultOrderEndsInAUniqueKey() {
        wire();
        givenTrail(List.of());

        service.getSubscriptionHistory(SCHOOL, "current", request());

        ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
        verify(history).search(anyString(), anyString(), any(), paging.capture());
        List<Sort.Order> orders = paging.getValue().getSort().toList();

        assertThat(orders).hasSize(3);
        assertThat(orders.get(0).getProperty()).isEqualTo("effectiveAt");
        assertThat(orders.get(1).getProperty()).isEqualTo("createdAt");
        // Unique, or paging is not stable: a tied row could appear on two pages and another never.
        assertThat(orders.get(2).getProperty()).isEqualTo("id");
        assertThat(orders).allMatch(o -> o.getDirection() == Sort.Direction.DESC);
    }

    @Test
    @DisplayName("a caller's sort goes first and keeps the default underneath as the tiebreaker")
    void callerSortKeepsTheTiebreaker() {
        wire();
        givenTrail(List.of());
        SubscriptionHistorySearchRequest sorted = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, "eventType,asc");

        service.getSubscriptionHistory(SCHOOL, "current", sorted);

        ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
        verify(history).search(anyString(), anyString(), any(), paging.capture());
        List<Sort.Order> orders = paging.getValue().getSort().toList();

        assertThat(orders.get(0).getProperty()).isEqualTo("eventType");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders.stream().map(Sort.Order::getProperty))
                .containsExactly("eventType", "effectiveAt", "createdAt", "id");
    }

    @Test
    @DisplayName("sorting on createdAt does not emit that key twice")
    void aNamedDefaultKeyIsNotEmittedTwice() {
        wire();
        givenTrail(List.of());
        SubscriptionHistorySearchRequest sorted = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, "createdAt,asc");

        service.getSubscriptionHistory(SCHOOL, "current", sorted);

        ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
        verify(history).search(anyString(), anyString(), any(), paging.capture());
        List<Sort.Order> orders = paging.getValue().getSort().toList();

        // The caller's ASC must win; a repeated key in a sort document keeps the last one.
        assertThat(orders.stream().filter(o -> o.getProperty().equals("createdAt")).count())
                .isEqualTo(1);
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    // -------------------------------------------------------- N+1

    @Test
    @DisplayName("a page of twenty plan changes costs ONE plan lookup, not forty")
    void oneQueryForEveryPlanOnThePage() {
        wire();
        List<SubscriptionHistory> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            rows.add(row("h" + i, "planA", "planB"));
        }
        givenTrail(rows);
        when(planDefinition.findAllById(any()))
                .thenReturn(List.of(plan("planA", "A"), plan("planB", "B")));

        service.getSubscriptionHistory(SCHOOL, "current", request());

        verify(planDefinition, times(1)).findAllById(any());
        verify(planDefinition, never()).findById(anyString());
    }

    @Test
    @DisplayName("both sides of every row go into one set of distinct ids")
    void bothSidesGoIntoOneDistinctSet() {
        wire();
        givenTrail(List.of(row("h1", "planA", "planB"), row("h2", "planB", "planC")));
        when(planDefinition.findAllById(any())).thenReturn(List.of(
                plan("planA", "A"), plan("planB", "B"), plan("planC", "C")));

        service.getSubscriptionHistory(SCHOOL, "current", request());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> ids = ArgumentCaptor.forClass(Set.class);
        verify(planDefinition).findAllById(ids.capture());

        // planB appears on both rows and is asked for once.
        assertThat(ids.getValue()).containsExactlyInAnyOrder("planA", "planB", "planC");
    }

    @Test
    @DisplayName("a page whose rows name no plan does not query plans at all")
    void noPlansMeansNoPlanQuery() {
        wire();
        givenTrail(List.of(row("h1", null, null)));

        service.getSubscriptionHistory(SCHOOL, "current", request());

        verify(planDefinition, never()).findAllById(any());
    }

    @Test
    @DisplayName("and it does not fall over on the empty plan map")
    void anEmptyPlanMapDoesNotThrow() {
        wire();
        givenTrail(List.of(row("h1", null, null)));

        // THE BUG THIS GUARDS: Map.of() is ImmutableCollections.MapN, whose get() rejects a null
        // key rather than answering null. So the page that needed NO plan lookup was the one that
        // threw NullPointerException. #28 had the same latent fault.
        PageResponse<SubscriptionHistoryEntryResponse> page =
                service.getSubscriptionHistory(SCHOOL, "current", request());

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).previousPlanCode()).isNull();
        assertThat(page.content().get(0).newPlanCode()).isNull();
    }

    // -------------------------------------------------------- what comes back

    @Test
    @DisplayName("both plans are named, not just linked")
    void bothPlansAreNamed() {
        wire();
        givenTrail(List.of(row("h1", "planA", "planB")));
        when(planDefinition.findAllById(any()))
                .thenReturn(List.of(plan("planA", "OLD"), plan("planB", "NEW")));

        var entry = service.getSubscriptionHistory(SCHOOL, "current", request()).content().get(0);

        assertThat(entry.previousPlanCode()).isEqualTo("OLD");
        assertThat(entry.newPlanCode()).isEqualTo("NEW");
        assertThat(entry.previousPlanName()).isEqualTo("Plan OLD");
        assertThat(entry.newPlanVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("a plan that has since been deleted leaves that side null, not the whole page")
    void aDeletedPlanLeavesThatSideNull() {
        wire();
        givenTrail(List.of(row("h1", "goneForever", "planB")));
        // Only planB comes back: the other plan document no longer exists.
        when(planDefinition.findAllById(any())).thenReturn(List.of(plan("planB", "NEW")));

        var entry = service.getSubscriptionHistory(SCHOOL, "current", request()).content().get(0);

        assertThat(entry.previousPlanCode()).isNull();
        assertThat(entry.newPlanCode()).isEqualTo("NEW");
    }

    @Test
    @DisplayName("the subscription number is on every row, read once for the whole page")
    void subscriptionNumberIsOnEveryRow() {
        wire();
        givenTrail(List.of(row("h1", null, null), row("h2", null, null)));

        var page = service.getSubscriptionHistory(SCHOOL, "current", request());

        assertThat(page.content()).allMatch(r -> SUB_NO.equals(r.subscriptionNo()));
        // Once for the subscription, and never again per row.
        verify(schoolSubscription, times(1)).findBySchoolIdAndCurrentIsTrue(SCHOOL);
    }

    @Test
    @DisplayName("an empty trail is an empty page, not an error")
    void anEmptyTrailIsAnEmptyPage() {
        wire();
        givenTrail(List.of());

        var page = service.getSubscriptionHistory(SCHOOL, "current", request());

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    @DisplayName("the row's two dates are reported separately and not swapped")
    void theTwoDatesAreNotSwapped() {
        wire();
        givenTrail(List.of(row("h1", null, null)));

        var entry = service.getSubscriptionHistory(SCHOOL, "current", request()).content().get(0);

        assertThat(entry.effectiveAt()).isEqualTo(Instant.parse("2026-09-10T00:00:00Z"));
        assertThat(entry.recordedAt()).isEqualTo(Instant.parse("2026-09-09T00:00:00Z"));
    }

    @Test
    @DisplayName("nothing about reading a trail writes")
    void readingNeverWrites() {
        wire();
        givenTrail(List.of(row("h1", null, null)));

        service.getSubscriptionHistory(SCHOOL, "current", request());

        verify(history, never()).save(any());
        verify(history, never()).saveAll(any());
        verify(history, never()).delete(any());
        verify(history, never()).deleteById(anyString());
        verify(schoolSubscription, never()).save(any());
    }
}
