package com.orbitastra.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.orbitastra.backend.common.error.exception.ApiException;

/**
 * {@link PageResponse#pageableOf} — the shared page/size/sort resolution behind every list
 * endpoint.
 *
 * <p>Worth unit-testing rather than only exercising through an endpoint: every list endpoint now
 * depends on it, so a mistake here is a mistake in all of them at once, and the interesting cases
 * (a sort key that collides with the tiebreaker) are awkward to set up over HTTP.
 */
class PageResponseTest {

    private static final Map<String, String> SORTABLE = new LinkedHashMap<>();

    static {
        SORTABLE.put("currentperiodstart", "currentPeriodStart");
        SORTABLE.put("subscriptionno", "subscriptionNo");
        SORTABLE.put("status", "status");
    }

    private static final String NAMES = "currentPeriodStart, subscriptionNo, status";

    /** Two orders, ending in a unique one — the shape a stable fallback has to have. */
    private static final Sort FALLBACK = Sort.by(
            Sort.Order.desc("currentPeriodStart"),
            Sort.Order.desc("subscriptionNo"));

    private Pageable resolve(Integer page, Integer size, String sort) {
        return PageResponse.pageableOf(page, size, sort, SORTABLE, NAMES, FALLBACK);
    }

    @Nested
    @DisplayName("page and size")
    class PageAndSize {

        @Test
        @DisplayName("absent page and size take the defaults")
        void defaults() {
            Pageable pageable = resolve(null, null, null);

            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(PageResponse.DEFAULT_SIZE);
        }

        @Test
        @DisplayName("what was asked for is what comes back")
        void honoursWhatWasAsked() {
            Pageable pageable = resolve(3, 5, null);

            assertThat(pageable.getPageNumber()).isEqualTo(3);
            assertThat(pageable.getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("page zero is fine; it is the first page, not a missing one")
        void pageZeroIsValid() {
            assertThat(resolve(0, 20, null).getPageNumber()).isZero();
        }

        @Test
        @DisplayName("a negative page is refused, naming what it received")
        void negativePage() {
            assertThatThrownBy(() -> resolve(-1, 20, null))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("page cannot be negative")
                    .hasMessageContaining("-1");
        }

        @Test
        @DisplayName("size below one is refused")
        void sizeTooSmall() {
            assertThatThrownBy(() -> resolve(0, 0, null))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("size must be between 1 and " + PageResponse.MAX_SIZE);
        }

        @Test
        @DisplayName("size of exactly one is allowed — the smallest real page")
        void sizeOneIsValid() {
            assertThat(resolve(0, 1, null).getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("the maximum size is allowed")
        void maximumSizeIsValid() {
            assertThat(resolve(0, PageResponse.MAX_SIZE, null).getPageSize())
                    .isEqualTo(PageResponse.MAX_SIZE);
        }

        @Test
        @DisplayName("one over the maximum is REFUSED, not silently clamped")
        void sizeAboveMaximumIsRefusedNotClamped() {
            assertThatThrownBy(() -> resolve(0, PageResponse.MAX_SIZE + 1, null))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining(String.valueOf(PageResponse.MAX_SIZE + 1));
        }
    }

    @Nested
    @DisplayName("sort")
    class SortResolution {

        @Test
        @DisplayName("no sort parameter leaves the fallback order exactly as it is")
        void absentSortUsesFallback() {
            assertThat(resolve(0, 20, null).getSort()).isEqualTo(FALLBACK);
        }

        @Test
        @DisplayName("a blank sort parameter is the same as none")
        void blankSortUsesFallback() {
            assertThat(resolve(0, 20, "   ").getSort()).isEqualTo(FALLBACK);
        }

        @Test
        @DisplayName("the caller's field leads, and the fallback follows as the tiebreaker")
        void callerFieldLeadsAndFallbackTiebreaks() {
            Sort sort = resolve(0, 20, "status,asc").getSort();

            assertThat(sort.stream().map(Sort.Order::getProperty))
                    .containsExactly("status", "currentPeriodStart", "subscriptionNo");
            assertThat(sort.getOrderFor("status").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("a bare field defaults to ascending")
        void bareFieldIsAscending() {
            assertThat(resolve(0, 20, "status").getSort().getOrderFor("status").getDirection())
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("the field name is case-insensitive")
        void fieldIsCaseInsensitive() {
            assertThat(resolve(0, 20, "CurrentPeriodStart,desc").getSort()
                    .getOrderFor("currentPeriodStart")).isNotNull();
        }

        @Test
        @DisplayName("the direction is case-insensitive")
        void directionIsCaseInsensitive() {
            assertThat(resolve(0, 20, "status,DESC").getSort().getOrderFor("status")
                    .getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("a field off the allow-list is refused, and the message lists the allowed ones")
        void unknownFieldIsRefused() {
            assertThatThrownBy(() -> resolve(0, 20, "contractedPrice,desc"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("'contractedPrice' cannot be sorted on")
                    .hasMessageContaining("currentPeriodStart");
        }

        @Test
        @DisplayName("a direction that is neither asc nor desc is refused")
        void unknownDirectionIsRefused() {
            assertThatThrownBy(() -> resolve(0, 20, "status,sideways"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("'sideways' is not a direction");
        }

        /**
         * THE REGRESSION THIS CLASS EXISTS FOR.
         *
         * <p>A Mongo sort is a document and cannot hold one key twice — the driver keeps the last.
         * So a fallback of {@code currentPeriodStart DESC} appended under a caller's
         * {@code currentPeriodStart ASC} used to produce two entries for that key, and the
         * ascending direction the caller asked for was silently thrown away. The plan catalogue
         * had shipped with {@code ?sort=planCode,desc} returning ascending order for exactly this
         * reason.
         */
        @Test
        @DisplayName("a field the caller named appears ONCE, in the caller's direction")
        void callerFieldIsNotDuplicatedByTheFallback() {
            Sort sort = resolve(0, 20, "currentPeriodStart,asc").getSort();

            assertThat(sort.stream().map(Sort.Order::getProperty)
                    .filter("currentPeriodStart"::equals)).hasSize(1);
            assertThat(sort.getOrderFor("currentPeriodStart").getDirection())
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("and the rest of the fallback still follows it as the tiebreaker")
        void tiebreakerSurvivesTheDeduplication() {
            Sort sort = resolve(0, 20, "currentPeriodStart,asc").getSort();

            assertThat(sort.stream().map(Sort.Order::getProperty))
                    .containsExactly("currentPeriodStart", "subscriptionNo");
        }

        @Test
        @DisplayName("sorting on the tiebreaker itself leaves a valid single-key order")
        void sortingOnTheTiebreakerItself() {
            Sort sort = resolve(0, 20, "subscriptionNo,asc").getSort();

            assertThat(sort.stream().map(Sort.Order::getProperty))
                    .containsExactly("subscriptionNo", "currentPeriodStart");
            assertThat(sort.getOrderFor("subscriptionNo").getDirection())
                    .isEqualTo(Sort.Direction.ASC);
        }
    }
}
