package com.orbitastra.backend.repositories.plans.subscriptionhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionHistorySearchRequest;
import com.orbitastra.backend.models.plans.SubscriptionHistory;
import com.orbitastra.backend.models.plans.enums.SubscriptionEventType;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * What #29's query actually asks the database.
 *
 * <p><b>Why this is worth testing at all.</b> A dropped filter returns <i>more</i> rows, and more
 * rows looks like data rather than a bug — nobody reading a page of audit history notices that
 * four rows should have been one. So the query document is captured and read, rather than the
 * result being eyeballed.
 *
 * <p>It also covers the one thing no HTTP-level test can reach: the tenant clause. A history row
 * is keyed by the subscription's id, which is globally unique, so filtering on the subscription
 * alone returns exactly the same rows and every end-to-end assertion still passes with the tenant
 * clause deleted. That was verified by deleting it — 113 live assertions, none of them noticed.
 * It is defence in depth, and this is the only place it can be defended.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionHistoryRepositoryImplTest {

    private static final String SCHOOL = "6aa15d9dc3f7d0033333333a";
    private static final String SUB = "6aa1a44dc3f7d0011223344b";

    @Mock
    private MongoTemplate mongo;

    @InjectMocks
    private SubscriptionHistoryRepositoryImpl repository;

    @Captor
    private ArgumentCaptor<Query> queries;

    /** No filters at all — the boundary and nothing else. */
    private static SubscriptionHistorySearchRequest empty() {
        return new SubscriptionHistorySearchRequest(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    /** The `$and` list the implementation builds, as plain documents. */
    @SuppressWarnings("unchecked")
    private List<Document> andClauses(Query query) {
        Document document = query.getQueryObject();
        assertThat(document).containsKey("$and");
        return (List<Document>) document.get("$and");
    }

    private Query capturePageQuery(SubscriptionHistorySearchRequest request) {
        when(mongo.find(any(Query.class), eq(SubscriptionHistory.class))).thenReturn(List.of());
        repository.search(SCHOOL, SUB, request, PageRequest.of(0, 20));
        verify(mongo).find(queries.capture(), eq(SubscriptionHistory.class));
        return queries.getValue();
    }

    // ----------------------------------------------------------------- the boundary

    @Test
    @DisplayName("the tenant is always in the query, even with no filters")
    void tenantIsAlwaysApplied() {
        List<Document> clauses = andClauses(capturePageQuery(empty()));

        assertThat(clauses).contains(new Document("schoolId", SCHOOL));
    }

    @Test
    @DisplayName("and so is the subscription")
    void subscriptionIsAlwaysApplied() {
        List<Document> clauses = andClauses(capturePageQuery(empty()));

        assertThat(clauses).contains(new Document("schoolSubscriptionDocsId", SUB));
    }

    @Test
    @DisplayName("no filters means exactly two clauses, not a wider query")
    void noFiltersMeansOnlyTheBoundary() {
        assertThat(andClauses(capturePageQuery(empty()))).hasSize(2);
    }

    @Test
    @DisplayName("the tenant survives every filter being sent at once")
    void tenantSurvivesEveryFilter() {
        SubscriptionHistorySearchRequest all = new SubscriptionHistorySearchRequest(
                List.of(SubscriptionEventType.SUSPENDED),
                List.of(SubscriptionStatus.SUSPENDED),
                List.of(SubscriptionStatus.ACTIVE),
                "ADMIN_PORTAL", "who", "event-1", "non-payment",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"),
                0, 20, null);

        assertThat(andClauses(capturePageQuery(all)))
                .contains(new Document("schoolId", SCHOOL));
    }

    // ----------------------------------------------------------------- the count

    @Test
    @DisplayName("the count carries the filter but never the paging")
    void countCarriesTheFilterWithoutThePaging() {
        when(mongo.find(any(Query.class), eq(SubscriptionHistory.class))).thenReturn(List.of());

        repository.search(SCHOOL, SUB, empty(), PageRequest.of(3, 5));

        ArgumentCaptor<Query> counted = ArgumentCaptor.forClass(Query.class);
        verify(mongo).count(counted.capture(), eq(SubscriptionHistory.class));

        // Skip and limit on a count would only ever count one page.
        assertThat(counted.getValue().getSkip()).isZero();
        assertThat(counted.getValue().getLimit()).isZero();
        assertThat(andClauses(counted.getValue())).contains(new Document("schoolId", SCHOOL));
    }

    @Test
    @DisplayName("the page query does carry the paging")
    void pageQueryCarriesThePaging() {
        when(mongo.find(any(Query.class), eq(SubscriptionHistory.class))).thenReturn(List.of());

        repository.search(SCHOOL, SUB, empty(), PageRequest.of(3, 5));
        verify(mongo).find(queries.capture(), eq(SubscriptionHistory.class));

        assertThat(queries.getValue().getSkip()).isEqualTo(15);
        assertThat(queries.getValue().getLimit()).isEqualTo(5);
    }

    // ----------------------------------------------------------------- each filter

    @Test
    @DisplayName("an absent filter adds no clause")
    void absentFiltersAddNothing() {
        SubscriptionHistorySearchRequest blanks = new SubscriptionHistorySearchRequest(
                List.of(), List.of(), List.of(), "  ", "", "   ", " ",
                null, null, null, null, null, null, null);

        // Empty lists and blank strings are "not sent", not "match the empty value".
        assertThat(andClauses(capturePageQuery(blanks))).hasSize(2);
    }

    @Test
    @DisplayName("eventType becomes an $in, so it ORs within itself")
    void eventTypeIsAnIn() {
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                List.of(SubscriptionEventType.SUSPENDED, SubscriptionEventType.RESUMED),
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        Document clause = andClauses(capturePageQuery(request)).stream()
                .filter(d -> d.containsKey("eventType")).findFirst().orElseThrow();

        assertThat(((Document) clause.get("eventType")).get("$in"))
                .isEqualTo(List.of(SubscriptionEventType.SUSPENDED, SubscriptionEventType.RESUMED));
    }

    @Test
    @DisplayName("the two status ends are separate fields, so both are applied")
    void bothStatusEndsAreApplied() {
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                null, List.of(SubscriptionStatus.SUSPENDED), List.of(SubscriptionStatus.ACTIVE),
                null, null, null, null, null, null, null, null, null, null, null);

        List<Document> clauses = andClauses(capturePageQuery(request));

        assertThat(clauses).anyMatch(d -> d.containsKey("newStatus"));
        assertThat(clauses).anyMatch(d -> d.containsKey("previousStatus"));
    }

    @Test
    @DisplayName("a reason is a regex with its metacharacters escaped")
    void reasonIsQuoted() {
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, ".*",
                null, null, null, null, null, null, null);

        Document clause = andClauses(capturePageQuery(request)).stream()
                .filter(d -> d.containsKey("reason")).findFirst().orElseThrow();

        // \Q...\E is what makes `.*` search for those two characters instead of matching all.
        assertThat(clause.get("reason").toString()).contains("\\Q.*\\E");
    }

    @Test
    @DisplayName("a source is anchored at both ends, so it is exact rather than a substring")
    void sourceIsAnchored() {
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                null, null, null, "ADMIN_PORTAL", null, null, null,
                null, null, null, null, null, null, null);

        Document clause = andClauses(capturePageQuery(request)).stream()
                .filter(d -> d.containsKey("source")).findFirst().orElseThrow();
        String pattern = clause.get("source").toString();

        assertThat(pattern).contains("^\\QADMIN_PORTAL\\E$");
    }

    @Test
    @DisplayName("both ends of a date window survive on one field")
    void bothEndsOfAWindowSurvive() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2027-03-31T23:59:59Z");
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null, from, to, null, null, null, null, null);

        Document clause = andClauses(capturePageQuery(request)).stream()
                .filter(d -> d.containsKey("effectiveAt")).findFirst().orElseThrow();
        Document window = (Document) clause.get("effectiveAt");

        // The bug this guards: if the two ends were merged into ONE document by key rather than
        // kept together, one of them would be lost. Inclusive at both ends on purpose.
        assertThat(window.get("$gte")).isEqualTo(from);
        assertThat(window.get("$lte")).isEqualTo(to);
    }

    @Test
    @DisplayName("the two windows are independent fields")
    void theTwoWindowsAreIndependent() {
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null,
                Instant.parse("2026-04-01T00:00:00Z"), null,
                Instant.parse("2026-05-01T00:00:00Z"), null, null, null, null);

        List<Document> clauses = andClauses(capturePageQuery(request));

        assertThat(clauses).anyMatch(d -> d.containsKey("effectiveAt"));
        assertThat(clauses).anyMatch(d -> d.containsKey("createdAt"));
    }

    @Test
    @DisplayName("one end of a window on its own is still applied")
    void oneEndedWindowIsApplied() {
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null,
                null, Instant.parse("2026-04-01T00:00:00Z"), null, null, null, null, null);

        Document clause = andClauses(capturePageQuery(request)).stream()
                .filter(d -> d.containsKey("effectiveAt")).findFirst().orElseThrow();
        Document window = (Document) clause.get("effectiveAt");

        assertThat(window).containsKey("$lte");
        assertThat(window).doesNotContainKey("$gte");
    }

    // ----------------------------------------------------------------- the $and question

    @Test
    @DisplayName("a window is one clause, and a same-field clause would not be lost anyway")
    void aWindowIsOneClause() {
        // WHY THIS TEST EXISTS. The comment inherited from #28 says two `where` clauses on one
        // field would lose one, because "a document cannot hold the same key twice". Half right:
        // that IS what happens in a SORT document, where it was a real bug. It is NOT what
        // happens here, and it was worth checking rather than repeating.
        //
        // What Spring Data actually produces, printed from a standalone run:
        //
        //   one Criteria chained    -> {effectiveAt: {$gte: .., $lte: ..}}
        //   andOperator of both     -> {$and: [{schoolId: S}, {effectiveAt: {$gte:.., $lte:..}}]}
        //   two separate Criteria   -> {$and: [{schoolId: S}, {effectiveAt: {$gte:..}},
        //                                                     {effectiveAt: {$lte:..}}]}
        //
        // $and is an ARRAY, so the split form keeps both ends too — splitting the window was
        // mutation-tested and changed no result. Keeping both ends on one Criteria is a clarity
        // choice, and it is the form that stays correct if these are ever merged with `.and()`
        // instead of and-ed, which is where a same-field key really does collide.
        SubscriptionHistorySearchRequest request = new SubscriptionHistorySearchRequest(
                null, null, null, null, null, null, null,
                Instant.parse("2026-04-01T00:00:00Z"), Instant.parse("2027-03-31T00:00:00Z"),
                null, null, null, null, null);

        List<Document> clauses = andClauses(capturePageQuery(request));

        // three: the tenant, the subscription, and the one window carrying both ends
        assertThat(clauses).hasSize(3);
        assertThat(clauses.stream().filter(d -> d.containsKey("effectiveAt")).count()).isEqualTo(1);
    }

    // ----------------------------------------------------------------- the order

    @Test
    @DisplayName("the sort the caller was given is the sort the query runs")
    void theSortIsPassedThrough() {
        when(mongo.find(any(Query.class), eq(SubscriptionHistory.class))).thenReturn(List.of());
        Sort order = Sort.by(Sort.Order.desc("effectiveAt"), Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));

        repository.search(SCHOOL, SUB, empty(), PageRequest.of(0, 20, order));
        verify(mongo).find(queries.capture(), eq(SubscriptionHistory.class));

        Document sort = queries.getValue().getSortObject();

        // The property, not the stored field: getSortObject() is the query before MongoTemplate's
        // QueryMapper runs, and that is what turns `id` into `_id` on the way to the server. The
        // ordering it produces is proved end to end instead — six rows sharing an instant page
        // through in one total order.
        assertThat(sort.keySet()).containsExactly("effectiveAt", "createdAt", "id");
        assertThat(sort.values()).containsExactly(-1, -1, -1);
    }

    @Test
    @DisplayName("nothing in the search writes")
    void searchOnlyReads() {
        when(mongo.find(any(Query.class), eq(SubscriptionHistory.class))).thenReturn(List.of());

        repository.search(SCHOOL, SUB, empty(), PageRequest.of(0, 20));

        verify(mongo).count(any(Query.class), eq(SubscriptionHistory.class));
        verify(mongo).find(any(Query.class), eq(SubscriptionHistory.class));
        org.mockito.Mockito.verifyNoMoreInteractions(mongo);
    }
}
