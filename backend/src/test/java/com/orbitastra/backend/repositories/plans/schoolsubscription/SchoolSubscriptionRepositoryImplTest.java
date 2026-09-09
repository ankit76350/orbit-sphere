package com.orbitastra.backend.repositories.plans.schoolsubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.orbitastra.backend.dto.plans.subscription.request.SubscriptionSearchRequest;
import com.orbitastra.backend.models.plans.SchoolSubscription;
import com.orbitastra.backend.models.plans.enums.BillingCycle;
import com.orbitastra.backend.models.plans.enums.SubscriptionStatus;

/**
 * #28's query, checked as a query rather than through its results.
 *
 * <p><b>Why assert on the Mongo document instead of the rows that come back.</b> Two of the
 * mistakes this code can make are invisible in the results: a filter silently dropped returns
 * <i>more</i> rows, which reads as "there were more than I thought", and a filter overwritten by
 * another on the same field returns a plausible subset. Both look like data. Capturing the
 * {@link Query} and reading its criteria is the only way to see that the filter actually asked
 * what it was supposed to.
 *
 * <p>No database: {@link MongoTemplate} is mocked, so these run in milliseconds and say nothing
 * about whether Mongo agrees — that is what the end-to-end suite is for.
 */
@ExtendWith(MockitoExtension.class)
class SchoolSubscriptionRepositoryImplTest {

    private static final String SCHOOL_ID = "6a9fea0f7feee1a04ace4b45";

    @Mock
    private MongoTemplate mongo;

    @Captor
    private ArgumentCaptor<Query> queries;

    private SchoolSubscriptionRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new SchoolSubscriptionRepositoryImpl(mongo);
        when(mongo.count(any(Query.class), eq(SchoolSubscription.class))).thenReturn(0L);
        when(mongo.find(any(Query.class), eq(SchoolSubscription.class))).thenReturn(List.of());
    }

    private static SubscriptionSearchRequest request(List<SubscriptionStatus> statuses,
            List<BillingCycle> cycles, Integer planVersion, Boolean autoRenew, Boolean current,
            Instant startFrom, Instant startTo, Instant endFrom, Instant endTo) {

        return new SubscriptionSearchRequest(statuses, cycles, null, planVersion, autoRenew,
                current, startFrom, startTo, endFrom, endTo, null, null, null);
    }

    private static SubscriptionSearchRequest bare() {
        return request(null, null, null, null, null, null, null, null, null);
    }

    /** The filter document the page query carried. */
    private Document filterOf(SubscriptionSearchRequest request,
            Collection<String> planIds, Pageable pageable) {

        repository.search(SCHOOL_ID, request, planIds, pageable);

        // Two calls: the count, then the page. Either carries the same criteria; the page one is
        // the one with the paging on it.
        org.mockito.Mockito.verify(mongo).find(queries.capture(), eq(SchoolSubscription.class));

        return queries.getValue().getQueryObject();
    }

    private Document filterOf(SubscriptionSearchRequest request) {
        return filterOf(request, null, PageRequest.of(0, 20));
    }

    /** The clauses inside the top-level {@code $and}, which is how the filter is assembled. */
    @SuppressWarnings("unchecked")
    private static List<Document> clauses(Document filter) {
        return (List<Document>) filter.get("$and");
    }

    private static Document clauseOn(Document filter, String field) {
        return clauses(filter).stream()
                .filter(clause -> clause.containsKey(field))
                .findFirst()
                .orElse(null);
    }

    @Test
    @DisplayName("the school id is always in the filter, even with no filters at all")
    void tenantIsAlwaysApplied() {
        Document filter = filterOf(bare());

        assertThat(clauseOn(filter, "schoolId")).isNotNull()
                .containsEntry("schoolId", SCHOOL_ID);
    }

    @Test
    @DisplayName("with no filters, the school id is the ONLY clause")
    void bareRequestFiltersOnNothingElse() {
        assertThat(clauses(filterOf(bare()))).hasSize(1);
    }

    @Test
    @DisplayName("the count query carries the filter but NOT the paging")
    void countCarriesNoPaging() {
        repository.search(SCHOOL_ID, bare(), null, PageRequest.of(3, 5));

        ArgumentCaptor<Query> counted = ArgumentCaptor.forClass(Query.class);
        org.mockito.Mockito.verify(mongo).count(counted.capture(), eq(SchoolSubscription.class));

        // Given the page's skip and limit it would only ever count one page.
        assertThat(counted.getValue().getLimit()).isZero();
        assertThat(counted.getValue().getSkip()).isZero();
        assertThat(clauses(counted.getValue().getQueryObject())).hasSize(1);
    }

    @Test
    @DisplayName("the page query carries the paging and the sort")
    void pageCarriesPagingAndSort() {
        repository.search(SCHOOL_ID, bare(), null,
                PageRequest.of(2, 5, Sort.by(Sort.Order.desc("currentPeriodStart"))));

        org.mockito.Mockito.verify(mongo).find(queries.capture(), eq(SchoolSubscription.class));

        assertThat(queries.getValue().getLimit()).isEqualTo(5);
        assertThat(queries.getValue().getSkip()).isEqualTo(10);
        assertThat(queries.getValue().getSortObject())
                .containsEntry("currentPeriodStart", -1);
    }

    @Test
    @DisplayName("statuses become an $in, so they OR within themselves")
    void statusesAreOred() {
        Document clause = clauseOn(filterOf(request(
                List.of(SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED),
                null, null, null, null, null, null, null, null)), "status");

        assertThat(clause).isNotNull();
        assertThat(((Document) clause.get("status")).get("$in"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .containsExactlyInAnyOrder(SubscriptionStatus.CANCELLED,
                        SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("an empty status list is no filter, not a filter matching nothing")
    void emptyStatusListIsNoFilter() {
        assertThat(clauseOn(filterOf(request(List.of(), null, null, null, null,
                null, null, null, null)), "status")).isNull();
    }

    @Test
    @DisplayName("billing cycles become an $in too")
    void cyclesAreOred() {
        assertThat(clauseOn(filterOf(request(null, List.of(BillingCycle.MONTHLY),
                null, null, null, null, null, null, null)), "billingCycle")).isNotNull();
    }

    @Test
    @DisplayName("planVersion, autoRenew and current are exact matches")
    void exactMatches() {
        Document filter = filterOf(request(null, null, 2, true, false,
                null, null, null, null));

        assertThat(clauseOn(filter, "planVersion")).containsEntry("planVersion", 2);
        assertThat(clauseOn(filter, "autoRenew")).containsEntry("autoRenew", true);
        assertThat(clauseOn(filter, "current")).containsEntry("current", false);
    }

    @Test
    @DisplayName("current=false is a real filter, not read as absent")
    void currentFalseIsAFilter() {
        // The trap: `if (request.current())` on a Boolean would treat false as "not asked for",
        // and ?current=false would silently return the live row along with the history.
        assertThat(clauseOn(filterOf(request(null, null, null, null, false,
                null, null, null, null)), "current")).containsEntry("current", false);
    }

    @Test
    @DisplayName("no planCode means no plan clause at all")
    void nullPlanIdsMeansNoClause() {
        assertThat(clauseOn(filterOf(bare(), null, PageRequest.of(0, 20)),
                "planDefinitionDocsId")).isNull();
    }

    @Test
    @DisplayName("resolved plan ids become an $in")
    void planIdsBecomeAnIn() {
        Document clause = clauseOn(filterOf(bare(), Set.of("p1", "p2"), PageRequest.of(0, 20)),
                "planDefinitionDocsId");

        assertThat(clause).isNotNull();
        assertThat(((Document) clause.get("planDefinitionDocsId")).get("$in"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.COLLECTION)
                .containsExactlyInAnyOrder("p1", "p2");
    }

    /**
     * An EMPTY collection means a planCode was sent and matched no plan. That has to filter to
     * nothing — an {@code $in []} — rather than being dropped, which would answer a filter that
     * matched nothing with the school's whole history.
     */
    @Test
    @DisplayName("an EMPTY plan id set still becomes a clause, so it matches nothing")
    void emptyPlanIdsStillFilters() {
        Document clause = clauseOn(filterOf(bare(), Set.of(), PageRequest.of(0, 20)),
                "planDefinitionDocsId");

        assertThat(clause).isNotNull();
        assertThat(((Document) clause.get("planDefinitionDocsId")).get("$in"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.COLLECTION)
                .isEmpty();
    }

    /**
     * THE TRAP THIS CLASS EXISTS FOR. A Mongo filter is a document, and two {@code where} clauses
     * on one field are two keys of the same name — the driver keeps the last. So a from/to pair
     * built as two separate criteria would filter on {@code to} alone and quietly return every
     * row before that date, including ones before {@code from}.
     */
    @Test
    @DisplayName("BOTH ends of a start window land in ONE clause, as $gte and $lte")
    void startWindowKeepsBothEnds() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2027-03-31T00:00:00Z");

        Document filter = filterOf(request(null, null, null, null, null,
                from, to, null, null));

        assertThat(clauses(filter))
                .filteredOn(clause -> clause.containsKey("currentPeriodStart"))
                .hasSize(1);
        assertThat((Document) clauseOn(filter, "currentPeriodStart").get("currentPeriodStart"))
                .containsEntry("$gte", from)
                .containsEntry("$lte", to);
    }

    @Test
    @DisplayName("and both ends of an end window do the same")
    void endWindowKeepsBothEnds() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");
        Instant to = Instant.parse("2027-03-31T00:00:00Z");

        Document filter = filterOf(request(null, null, null, null, null,
                null, null, from, to));

        assertThat((Document) clauseOn(filter, "currentPeriodEnd").get("currentPeriodEnd"))
                .containsEntry("$gte", from)
                .containsEntry("$lte", to);
    }

    @Test
    @DisplayName("one end on its own is a one-sided bound")
    void oneEndedWindow() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");

        Document bound = (Document) clauseOn(filterOf(request(null, null, null, null, null,
                from, null, null, null)), "currentPeriodStart").get("currentPeriodStart");

        assertThat(bound).containsEntry("$gte", from).doesNotContainKey("$lte");
    }

    @Test
    @DisplayName("the bounds are INCLUSIVE, so a period starting on the from date is in")
    void boundsAreInclusive() {
        Instant from = Instant.parse("2026-04-01T00:00:00Z");

        Document bound = (Document) clauseOn(filterOf(request(null, null, null, null, null,
                from, null, null, null)), "currentPeriodStart").get("currentPeriodStart");

        // $gte rather than $gt: a caller filtering "from 1 April" means a period that begins on
        // 1 April is included, and an exclusive bound would drop the row they were looking for.
        assertThat(bound).containsKey("$gte").doesNotContainKey("$gt");
    }

    @Test
    @DisplayName("the start and end windows are separate clauses and do not collide")
    void startAndEndWindowsCoexist() {
        Document filter = filterOf(request(null, null, null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                Instant.parse("2027-12-31T00:00:00Z")));

        assertThat(clauseOn(filter, "currentPeriodStart")).isNotNull();
        assertThat(clauseOn(filter, "currentPeriodEnd")).isNotNull();
    }

    @Test
    @DisplayName("everything at once ANDs into one filter, with the tenant still in it")
    void everythingTogether() {
        Document filter = filterOf(request(
                List.of(SubscriptionStatus.ACTIVE), List.of(BillingCycle.YEARLY), 3, true, true,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-31T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2027-12-31T00:00:00Z")),
                Set.of("p1"), PageRequest.of(0, 20));

        assertThat(clauses(filter)).hasSize(9);
        assertThat(clauseOn(filter, "schoolId")).containsEntry("schoolId", SCHOOL_ID);
    }
}
