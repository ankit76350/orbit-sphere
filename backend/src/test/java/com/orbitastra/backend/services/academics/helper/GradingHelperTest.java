package com.orbitastra.backend.services.academics.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;

/**
 * {@link GradingHelper} — the band rules behind #1.
 *
 * <p>Worth unit-testing rather than only exercising over HTTP: a band set that <i>validates</i>
 * but resolves wrongly is the failure mode a create-only module cannot see, and the interesting
 * cases — a band touching another at a boundary, a scale walked from zero — are arithmetic that is
 * far easier to state here than to set up as a request.
 */
class GradingHelperTest {

    private final GradingHelper helper = new GradingHelper();

    /**
     * A band as it is STORED — which is what every check here now takes.
     *
     * <p>There were two builders until 2026-09-14, one per shape. They collapsed into this when #3
     * arrived: a PATCH validates the scheme it is about to <i>become</i>, so the checks had to read
     * the stored bands it did not send alongside the ones it did. #1 loses nothing by building its
     * documents first — it then validates exactly what it will save.
     */
    private static GradeBand stored(String code, String min, String max) {
        return GradeBand.builder()
                .gradeCode(code)
                .minimumValue(min == null ? null : new BigDecimal(min))
                .maximumValue(max == null ? null : new BigDecimal(max))
                .passed(true)
                .build();
    }

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    @Nested
    @DisplayName("the scale decides whether a ceiling is required")
    class ScaleCeiling {

        @Test
        void percentage_needs_one() {
            assertThatThrownBy(() ->
                    helper.validateScaleCeiling(GradingScaleType.PERCENTAGE, null))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("needs a maximumValue");
        }

        @Test
        void descriptor_refuses_one() {
            assertThatThrownBy(() ->
                    helper.validateScaleCeiling(GradingScaleType.DESCRIPTOR, n("100")))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("nothing to measure");
        }

        @Test
        void descriptor_without_one_is_fine() {
            helper.validateScaleCeiling(GradingScaleType.DESCRIPTOR, null);
        }

        @Test
        void a_ceiling_of_zero_is_not_a_scale() {
            assertThatThrownBy(() ->
                    helper.validateScaleCeiling(GradingScaleType.MARKS, BigDecimal.ZERO))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("must be above 0");
        }
    }

    @Nested
    @DisplayName("bounds are required or refused, never optional")
    class Bounds {

        @Test
        void a_measured_band_needs_both() {
            assertThatThrownBy(() -> helper.validateBandBounds(GradingScaleType.PERCENTAGE,
                    List.of(stored("A1", "91", null))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("one bound alone is not a range");
        }

        @Test
        void a_descriptor_band_must_have_neither() {
            assertThatThrownBy(() -> helper.validateBandBounds(GradingScaleType.DESCRIPTOR,
                    List.of(stored("SECURE", "0", "1"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("chosen rather than computed");
        }

        @Test
        void a_descriptor_band_with_neither_passes() {
            helper.validateBandBounds(GradingScaleType.DESCRIPTOR,
                    List.of(stored("SECURE", null, null)));
        }
    }

    @Nested
    @DisplayName("overlap is refused, and touching counts as overlapping")
    class Overlap {

        @Test
        void bands_sharing_a_boundary_overlap() {
            // Both bounds are inclusive, so 90 belongs to both. Contrast term dates, where a term
            // ending on the 30th and one starting on the 1st share no day.
            assertThatThrownBy(() -> helper.validateNoBandOverlap(GradingScaleType.PERCENTAGE,
                    List.of(stored("A2", "81", "90"), stored("A1", "90", "100"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("both cover 90");
        }

        @Test
        void adjacent_bands_do_not() {
            helper.validateNoBandOverlap(GradingScaleType.PERCENTAGE,
                    List.of(stored("A2", "81", "90"), stored("A1", "91", "100")));
        }

        @Test
        void order_in_the_list_does_not_matter() {
            // The set is sorted before the walk, so a school listing A1 first is checked the same.
            assertThatThrownBy(() -> helper.validateNoBandOverlap(GradingScaleType.PERCENTAGE,
                    List.of(stored("A1", "90", "100"), stored("A2", "81", "90"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("both cover 90");
        }

        @Test
        void a_band_swallowing_another_is_caught() {
            assertThatThrownBy(() -> helper.validateNoBandOverlap(GradingScaleType.PERCENTAGE,
                    List.of(stored("ALL", "0", "100"), stored("A1", "91", "100"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("both cover");
        }
    }

    @Nested
    @DisplayName("a gap is reported, never refused")
    class Gaps {

        @Test
        void the_ordinary_cbse_scale_warns_about_nothing() {
            // Adjacent bands ALWAYS leave a sliver — inclusive bounds mean 90 and 91 cannot meet
            // without overlapping. No whole mark fits in it, so nothing is said. This is the most
            // common scheme in the country and a warning on it would be a warning nobody reads.
            String warning = helper.gapWarning(GradingScaleType.PERCENTAGE, n("100"), List.of(
                    stored("E", "0", "32"),
                    stored("D", "33", "80"),
                    stored("A", "81", "100")));

            assertThat(warning).isNull();
        }

        @Test
        void a_sliver_alone_is_never_reported() {
            assertThat(helper.gapWarning(GradingScaleType.PERCENTAGE, n("100"),
                    List.of(stored("LOW", "0", "50.4"), stored("HIGH", "50.6", "100")))).isNull();
        }

        @Test
        void a_hole_in_the_middle_is_named() {
            String warning = helper.gapWarning(GradingScaleType.PERCENTAGE, n("100"), List.of(
                    stored("E", "0", "32"),
                    stored("A2", "81", "90"),
                    stored("A1", "91", "100")));

            // 32 to 81 holds 33 through 80 and is reported. 90 to 91 holds no whole mark and
            // is not — the one is a hole a school can see, the other is arithmetic.
            assertThat(warning)
                    .contains("32 to 81")
                    .doesNotContain("90 to 91")
                    .contains("warning rather than a refusal");
        }

        @Test
        void a_scale_that_stops_short_of_its_ceiling_is_named() {
            String warning = helper.gapWarning(GradingScaleType.PERCENTAGE, n("100"),
                    List.of(stored("PASS", "0", "60")));

            assertThat(warning).contains("60 to 100");
        }

        @Test
        void an_ungraded_ceiling_is_caught_even_though_it_is_one_mark() {
            // The trailing edge is CLOSED at the ceiling: a top band ending at 99 leaves 100
            // without a grade, and no other edge would notice.
            assertThat(helper.gapWarning(GradingScaleType.PERCENTAGE, n("100"),
                    List.of(stored("A", "0", "99")))).contains("99 to 100");
        }

        @Test
        void an_ungraded_zero_is_caught_the_same_way() {
            // The leading edge is CLOSED at 0, so a scale starting at 1 has left 0 out.
            assertThat(helper.gapWarning(GradingScaleType.PERCENTAGE, n("100"),
                    List.of(stored("A", "1", "100")))).contains("0 to 1");
        }

        @Test
        void a_descriptor_scheme_has_no_scale_to_leave_holes_in() {
            assertThat(helper.gapWarning(GradingScaleType.DESCRIPTOR, null,
                    List.of(stored("SECURE", null, null)))).isNull();
        }
    }

    @Nested
    @DisplayName("the remaining refusals")
    class Refusals {

        @Test
        void an_inverted_band_is_caught_before_overlap_can_mislabel_it() {
            assertThatThrownBy(() ->
                    helper.validateBandRanges(List.of(stored("A1", "100", "91"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("which is backwards");
        }

        @Test
        void equal_bounds_are_a_legal_one_value_band() {
            helper.validateBandRanges(List.of(stored("PERFECT", "100", "100")));
        }

        @Test
        void a_repeated_code_is_refused_case_insensitively() {
            assertThatThrownBy(() -> helper.validateBandCodesUnique(
                    List.of(stored("A1", "91", "100"), stored("a1", "81", "90"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("more than once");
        }

        @Test
        void a_band_past_the_ceiling_is_refused() {
            assertThatThrownBy(() -> helper.validateBandsWithinScale(
                    GradingScaleType.MARKS, n("7"), List.of(stored("EIGHT", "7.5", "8"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("past this scheme's ceiling of 7");
        }

        @Test
        void a_band_below_zero_is_refused() {
            assertThatThrownBy(() -> helper.validateBandsWithinScale(
                    GradingScaleType.PERCENTAGE, n("100"), List.of(stored("X", "-1", "10"))))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("below zero");
        }
    }
}
