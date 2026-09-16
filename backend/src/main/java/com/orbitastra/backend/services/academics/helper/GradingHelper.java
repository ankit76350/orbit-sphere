package com.orbitastra.backend.services.academics.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.enums.GradingScaleType;
import com.orbitastra.backend.models.academics.grading.embedded.GradeBand;

import lombok.RequiredArgsConstructor;

/**
 * The grading module's helper — the rules MongoDB cannot express about a band set.
 *
 * <p><b>Its own class rather than a section of {@link AcademicsHelper}.</b> That one is explicitly
 * "the rules MongoDB cannot express about terms" and every method there takes a term list. Band
 * coherence shares nothing with it but a module name, and a helper that validated two unrelated
 * documents would be a folder, not a class.
 *
 * <p><b>The scale is the first argument to almost everything here</b>, because it decides whether
 * the other arguments mean anything. A {@code DESCRIPTOR} scheme has no ceiling and no bounds, so
 * "is this band inside the scale" is not a stricter version of the same question — it is a
 * question that does not apply.
 *
 * <p><b>Each check stands on its own and calls nothing else in this class</b>, per the service
 * code-writing rules — the same convention {@link AcademicsHelper} follows. The one exception is
 * {@code plain}, which formats a number for a message and decides nothing.
 *
 * <p><b>Checks throw rather than return a flag.</b> The exception is {@link #gapWarning}, which
 * deliberately does not — see its note.
 *
 * <p><b>Every method here takes {@link GradeBand}, the stored type, rather than the request.</b>
 * That was {@code gapWarning}'s shape from the start and the rest followed on 2026-09-14, when #3
 * arrived: a PATCH validates the scheme it is about to <i>become</i>, which is the stored bands it
 * did not send merged with the ones it did. Taking the request would have meant either an overload
 * of every check or converting a document back into a body to validate it — and #1 loses nothing,
 * because it builds the documents first and then checks what it will actually save.
 */
@Component
@RequiredArgsConstructor
public class GradingHelper {

    /** Nothing may be graded below zero, on any scale this module supports. */
    private static final BigDecimal FLOOR = BigDecimal.ZERO;

    /** 91.00 reads as "91" in a message, and 7.50 as "7.5". */
    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    //! schemes — used by endpoint 1 ----------------------------------------------------

    /**
     * A scale that measures needs a ceiling; one that does not must not carry one.
     *
     * <p>{@code PERCENTAGE} and {@code MARKS} both resolve a mark by range, so
     * {@code maximumValue} is what "inside the scale" means and a band cannot be checked without
     * it. {@code DESCRIPTOR} has no mark to compare — a teacher picks "Developing" directly — so
     * a ceiling on it would be a number nothing could ever read.
     *
     * <p><b>Checked before any band is looked at.</b> A band's bounds are refused or required
     * depending on this answer, so asking about a band first would give the right refusal for the
     * wrong reason: "this band needs a minimum" when the truth is "this scheme measures nothing".
     *
     * Used by:
     * - createScheme()
     * - updateScheme()
     */
    public void validateScaleCeiling(GradingScaleType scaleType, BigDecimal maximumValue) {
        if (scaleType == GradingScaleType.DESCRIPTOR) {
            if (maximumValue != null) {
                throw ApiException.badRequest("GRADE_BAND_BOUNDS_NOT_ALLOWED",
                        "A DESCRIPTOR scheme has nothing to measure, so it cannot carry a "
                                + "maximumValue. Leave it out, or use PERCENTAGE or MARKS.");
            }
            return;
        }

        if (maximumValue == null) {
            throw ApiException.badRequest("SCALE_MAXIMUM_REQUIRED",
                    "A " + scaleType + " scheme needs a maximumValue — the ceiling its bands are "
                            + "read against. 100 for a percentage, 7 for an IB point scale.");
        }

        if (maximumValue.compareTo(FLOOR) <= 0) {
            throw ApiException.badRequest("SCALE_MAXIMUM_REQUIRED",
                    "maximumValue must be above 0. Received: " + plain(maximumValue) + ".");
        }
    }

    /**
     * Every band carries bounds, or none does, according to the scale.
     *
     * <p>Bean validation cannot express "required unless a sibling field says otherwise", which is
     * why {@code GradeBand.minimumValue} stopped being {@code @NotNull} on 2026-09-13 — it made
     * {@code DESCRIPTOR} impossible to store. The rule lives here instead, where the scale is
     * visible.
     *
     * <p><b>A half-bounded band is refused by this too.</b> A minimum with no maximum is not a
     * range, and treating the missing end as the scheme's ceiling would be inventing a boundary
     * the school never wrote.
     *
     * Used by:
     * - createScheme()
     * - updateScheme()
     */
    public void validateBandBounds(GradingScaleType scaleType, List<GradeBand> bands) {
        boolean measured = scaleType != GradingScaleType.DESCRIPTOR;

        for (GradeBand band : bands) {
            boolean hasMin = band.getMinimumValue() != null;
            boolean hasMax = band.getMaximumValue() != null;

            if (measured && !(hasMin && hasMax)) {
                throw ApiException.badRequest("GRADE_BAND_BOUNDS_REQUIRED",
                        "Band '" + band.getGradeCode() + "' needs both minimumValue and maximumValue "
                                + "on a " + scaleType + " scheme"
                                + (hasMin || hasMax ? " — one bound alone is not a range." : "."));
            }

            if (!measured && (hasMin || hasMax)) {
                throw ApiException.badRequest("GRADE_BAND_BOUNDS_NOT_ALLOWED",
                        "Band '" + band.getGradeCode() + "' carries a bound, but a DESCRIPTOR grade "
                                + "is chosen rather than computed. Remove the bounds, or use "
                                + "PERCENTAGE or MARKS.");
            }
        }
    }

    /**
     * A band's upper bound cannot precede its lower one.
     *
     * <p>Equal bounds are legal: a band covering exactly 100 is odd but not wrong, and refusing it
     * would be inventing a rule the model does not state. The same call the term range check
     * makes, for the same reason.
     *
     * <p><b>Checked before containment and before overlap.</b> An inverted band checked last gets
     * reported as an overlap — which is true, and says the wrong thing about which band is wrong.
     *
     * Used by:
     * - createScheme()
     * - updateScheme()
     */
    public void validateBandRanges(List<GradeBand> bands) {
        for (GradeBand band : bands) {
            if (band.getMinimumValue() == null || band.getMaximumValue() == null) {
                continue;
            }
            if (band.getMaximumValue().compareTo(band.getMinimumValue()) < 0) {
                throw ApiException.badRequest("INVALID_GRADE_BAND_RANGE",
                        "Band '" + band.getGradeCode() + "' runs from "
                                + plain(band.getMinimumValue()) + " to "
                                + plain(band.getMaximumValue()) + ", which is backwards.");
            }
        }
    }

    /**
     * No two bands of one scheme may share a {@code gradeCode}.
     *
     * <p>Case-folded: a scheme holding both "A1" and "a1" is a data-entry mistake in every school,
     * never a structure anybody designed. The same reasoning the term code check uses.
     *
     * <p>Unlike a term code this is not backed by an index — a band is embedded, so there is
     * nothing for a unique index to be declared on. <b>This check is the only enforcement</b>,
     * which is the opposite situation from the term codes, where the check exists to turn a
     * duplicate-key 500 into a 409.
     *
     * Used by:
     * - createScheme()
     * - updateScheme()
     */
    public void validateBandCodesUnique(List<GradeBand> bands) {
        Set<String> seen = new HashSet<>();

        for (GradeBand band : bands) {
            if (!seen.add(band.getGradeCode().trim().toUpperCase(Locale.ROOT))) {
                throw ApiException.conflict("GRADE_BAND_CODE_TAKEN",
                        "This scheme names '" + band.getGradeCode().trim() + "' more than once. A "
                                + "grade code identifies one band, so two rows carrying it "
                                + "describe no scale at all.");
            }
        }
    }

    /**
     * No band may reach outside the scale it belongs to.
     *
     * <p>Below zero or above {@code maximumValue} is a band that can never be reached by a mark,
     * which means a school looking at its own table sees a grade it will never award and cannot
     * tell why. Both ends are checked here rather than one in the DTO, because the ceiling is the
     * scheme's and a request record cannot see its own parent.
     *
     * <p>Skipped entirely for {@code DESCRIPTOR}, which has no scale to be outside of.
     *
     * Used by:
     * - createScheme()
     * - updateScheme()
     */
    public void validateBandsWithinScale(GradingScaleType scaleType, BigDecimal maximumValue,
            List<GradeBand> bands) {

        if (scaleType == GradingScaleType.DESCRIPTOR) {
            return;
        }

        for (GradeBand band : bands) {
            if (band.getMinimumValue().compareTo(FLOOR) < 0) {
                throw ApiException.conflict("GRADE_BAND_OUTSIDE_SCALE",
                        "Band '" + band.getGradeCode() + "' starts at "
                                + plain(band.getMinimumValue()) + ", below zero. Nothing is graded "
                                + "below zero.");
            }
            if (band.getMaximumValue().compareTo(maximumValue) > 0) {
                throw ApiException.conflict("GRADE_BAND_OUTSIDE_SCALE",
                        "Band '" + band.getGradeCode() + "' reaches "
                                + plain(band.getMaximumValue()) + ", past this scheme's ceiling of "
                                + plain(maximumValue) + ". A mark can never land there.");
            }
        }
    }

    /**
     * No two bands of one scheme may cover the same value.
     *
     * <p><b>Refused, where a gap is only reported</b> — see {@link #gapWarning}, and the asymmetry
     * is the whole point. A gap means one mark has <i>no</i> grade: visible, and fixable by
     * whoever reads the warning. An overlap means one mark has <i>two</i>, and which one wins
     * depends on the order the bands happen to be stored in — silently, and differently per
     * scheme. One is a hole somebody can see; the other is a coin toss nobody can.
     *
     * <p>Both bounds are inclusive, so bands touching at a boundary <b>do</b> overlap: 81–90
     * beside 90–100 both claim 90. Contrast term dates, where touching is fine because a term
     * ending on the 30th and one starting on the 1st share no day.
     *
     * <p>Skipped for {@code DESCRIPTOR}, whose bands cover nothing.
     *
     * Used by:
     * - createScheme()
     * - updateScheme()
     */
    public void validateNoBandOverlap(GradingScaleType scaleType, List<GradeBand> bands) {
        if (scaleType == GradingScaleType.DESCRIPTOR) {
            return;
        }

        List<GradeBand> ordered = bands.stream()
                .sorted(Comparator.comparing(GradeBand::getMinimumValue))
                .toList();

        for (int i = 1; i < ordered.size(); i++) {
            GradeBand lower = ordered.get(i - 1);
            GradeBand upper = ordered.get(i);

            if (upper.getMinimumValue().compareTo(lower.getMaximumValue()) <= 0) {
                throw ApiException.conflict("GRADE_BANDS_OVERLAP",
                        "Bands '" + lower.getGradeCode() + "' (" + plain(lower.getMinimumValue())
                                + "–" + plain(lower.getMaximumValue()) + ") and '"
                                + upper.getGradeCode() + "' (" + plain(upper.getMinimumValue())
                                + "–" + plain(upper.getMaximumValue())
                                + ") both cover " + plain(upper.getMinimumValue())
                                + ". Bounds are inclusive at both ends, so bands must not touch.");
            }
        }
    }

    /**
     * What to say when the bands leave part of the scale ungraded.
     *
     * <p><b>Returns a message instead of throwing, and that is the whole point.</b> The model
     * README asks services to reject "gaps that are not intentional", which is not something a
     * service can tell apart: 81–89 beside 91–100 leaves 90 unresolvable and is almost certainly a
     * typo, while a scheme that grades 0–32 as E and says nothing above 90 because no paper is
     * marked that high is perfectly deliberate. Refusing would make the second impossible to
     * express; reporting makes the first easy to see.
     *
     * <h2>Gaps are measured in whole marks, and they have to be</h2>
     *
     * <p><b>Both bounds are inclusive, so a continuous scale can never be tiled.</b> 81–90 beside
     * 91–100 leaves the sliver between 90 and 91 ungraded; closing it by writing 81–90 and 90–100
     * is an overlap, which is refused. There is no third option — every percentage scheme ever
     * written has slivers, including the CBSE one this project's examples are drawn from.
     *
     * <p>So a gap is only reported when <b>a whole mark can land in it</b>. 90 → 91 holds no
     * integer and is silent; 32 → 81 holds 33 through 80 and is not. Measured in marks the school
     * actually awards, rather than in real numbers nobody enters.
     *
     * <p><b>The cost is fractional marks.</b> A school awarding 90.5 gets no warning that 90.5
     * has no grade — #8 still answers {@code 404} for it, which is where it is discoverable.
     * Warning about it here would mean warning about every scheme, and a warning that is always
     * present is one nobody reads. See open item 5 in the module README.
     *
     * <p><b>The three edges are not the same question.</b> The scale is {@code [0, maximumValue]},
     * both ends gradeable — so a leading gap is closed at 0, a middle gap is open at both ends
     * (the bands claim their own boundaries), and a trailing gap is closed at the ceiling. A
     * scheme whose top band ends at 99 has left 100 ungraded, and that is worth saying.
     *
     * <p>Returns null for {@code DESCRIPTOR}, which has no scale to leave holes in.
     *
     * <h2>It takes stored bands, not a request</h2>
     *
     * <p><b>The only check here that does</b>, and the reason is #7: a scheme is read back far more
     * often than it is written, and the warning has to be <b>recomputed on every read</b> rather
     * than stored. A stored sentence would outlive the problem it described — a school that fixed
     * its bands would keep being told about a hole that is no longer there.
     *
     * <p>So #1 computes it from the document it just saved rather than from the body it was sent.
     * The two are the same set, and taking the saved one means this method has a single signature
     * instead of an overload per caller, and describes what is actually in the database.
     *
     * @return the warning, or null when every whole mark has a grade
     *
     * Used by:
     * - createScheme()
     * - getScheme()
     * - setActive()
     * - updateScheme()
     */
    public String gapWarning(GradingScaleType scaleType, BigDecimal maximumValue,
            List<GradeBand> bands) {

        if (scaleType == GradingScaleType.DESCRIPTOR) {
            return null;
        }

        List<GradeBand> ordered = bands.stream()
                .sorted(Comparator.comparing(GradeBand::getMinimumValue))
                .toList();

        List<String> gaps = new ArrayList<>();
        BigDecimal covered = FLOOR;
        boolean coveredIsClaimed = false;

        //! Walk the scale from zero. A band starting above where the last one ended leaves the
        //! span between them ungraded - but only worth saying when a whole mark fits in it,
        //! because inclusive bounds mean adjacent bands ALWAYS leave a sliver.
        for (GradeBand band : ordered) {
            if (band.getMinimumValue().compareTo(covered) > 0
                    && containsWholeMark(covered, !coveredIsClaimed,
                            band.getMinimumValue(), false)) {

                gaps.add(plain(covered) + " to " + plain(band.getMinimumValue()));
            }
            if (band.getMaximumValue().compareTo(covered) > 0) {
                covered = band.getMaximumValue();
                coveredIsClaimed = true;
            }
        }

        //! The ceiling is gradeable, so the trailing gap is CLOSED at it: a top band ending at 99
        //! has left 100 without a grade, which no other edge would catch.
        if (covered.compareTo(maximumValue) < 0
                && containsWholeMark(covered, !coveredIsClaimed, maximumValue, true)) {

            gaps.add(plain(covered) + " to " + plain(maximumValue));
        }

        if (gaps.isEmpty()) {
            return null;
        }

        return "Part of this scale has no grade: " + String.join(", ", gaps)
                + ". A mark landing there resolves to nothing, which #8 reports as "
                + "GRADE_NOT_RESOLVABLE. This is a warning rather than a refusal, because a gap "
                + "a school left on purpose is indistinguishable from one it did not mean.";
    }

    //! resolving — used by endpoint 8 ------------------------------------------------

    /**
     * The band one value falls in — the arithmetic every rule above exists to protect.
     *
     * <p><b>This is why #1's checks are worth enforcing.</b> A band set that <i>validates</i> but
     * <i>resolves</i> wrongly is the failure mode a create-only module cannot see, which is why
     * the plan puts #8 in phase 1 beside the write rather than after it.
     *
     * <p><b>It lives here rather than inline in the service, and that is a deliberate exception</b>
     * to "single-use logic stays inline". Every other method in this class is band arithmetic with
     * a unit test behind it, and resolution is the one piece where being subtly wrong is both
     * easiest and least visible — a boundary that resolves to the neighbouring band is invisible
     * over HTTP and obvious in a table of cases. {@code GradingHelperTest} is the reason.
     *
     * <h2>The three refusals are ordered, and the order is the answer</h2>
     *
     * <ol>
     *   <li><b>Can this scheme be resolved by value at all?</b> {@code DESCRIPTOR} cannot — a
     *       teacher picks "Developing" directly, so there is no arithmetic to do. {@code 409},
     *       because the request is coherent and the scheme is the wrong kind for it.</li>
     *   <li><b>Is the value on the scale?</b> Below zero or above the ceiling is {@code 400}: the
     *       caller sent a number this scheme could never produce.</li>
     *   <li><b>Does a band cover it?</b> If not, {@code 404} — the grade asked for does not
     *       exist, and the message names the two bands it fell between so the hole is visible in
     *       the school's own table.</li>
     * </ol>
     *
     * <p>Asking them in any other order gives a true answer to the wrong question: a descriptor
     * scheme asked about 90 would report "outside the scale" when it has no scale.
     *
     * <p><b>Both bounds are inclusive</b>, so a value equal to a boundary resolves to the band
     * that declares it. That is unambiguous only because {@link #validateNoBandOverlap} refuses
     * two bands claiming one value — the two rules are halves of one decision.
     *
     * <p><b>A retired scheme resolves.</b> {@code active} governs what is offered for new work;
     * a report card issued in 2026 reprints through the 2026 rules forever.
     *
     * Used by:
     * - resolveGrade()
     */
    public GradeBand resolveBand(GradingScaleType scaleType, BigDecimal maximumValue,
            List<GradeBand> bands, BigDecimal value) {

        //! step 1 - is there arithmetic to do at all
        if (scaleType == GradingScaleType.DESCRIPTOR) {
            throw ApiException.conflict("SCHEME_NOT_RESOLVABLE_BY_VALUE",
                    "This is a DESCRIPTOR scheme: a grade is chosen, not computed, so there is no "
                            + "value to resolve. Read the scheme and pick a band.");
        }

        //! step 2 - is the value on the scale. Checked before the bands, because "no band covers
        //! 150" is a true statement that hides the real problem: 150 is not a mark this scheme
        //! could ever produce.
        if (value.compareTo(FLOOR) < 0 || value.compareTo(maximumValue) > 0) {
            throw ApiException.badRequest("VALUE_OUTSIDE_SCALE",
                    plain(value) + " is outside this scheme's scale of 0 to "
                            + plain(maximumValue) + ".");
        }

        //! step 3 - the band. Both bounds inclusive, which is unambiguous only because an overlap
        //! is refused at write.
        for (GradeBand band : bands) {
            if (value.compareTo(band.getMinimumValue()) >= 0
                    && value.compareTo(band.getMaximumValue()) <= 0) {

                return band;
            }
        }

        //! step 4 - nothing covered it, so name the hole. A gap is a 404 rather than a 409: the
        //! caller asked for the grade at this value and there is none.
        //!
        //! THE NEIGHBOURS ARE IN THE MESSAGE because the school is the only one who can fix this,
        //! and "no grade for 90.5" does not say where to look. Computed from the band set rather
        //! than assumed to be sorted - nothing re-sorts a stored scheme.
        GradeBand below = null;
        GradeBand above = null;
        for (GradeBand band : bands) {
            if (band.getMaximumValue().compareTo(value) < 0
                    && (below == null
                            || band.getMaximumValue().compareTo(below.getMaximumValue()) > 0)) {
                below = band;
            }
            if (band.getMinimumValue().compareTo(value) > 0
                    && (above == null
                            || band.getMinimumValue().compareTo(above.getMinimumValue()) < 0)) {
                above = band;
            }
        }

        String between;
        if (below != null && above != null) {
            between = " It falls between '" + below.getGradeCode() + "' (ending "
                    + plain(below.getMaximumValue()) + ") and '" + above.getGradeCode()
                    + "' (starting " + plain(above.getMinimumValue()) + ").";
        } else if (below != null) {
            between = " The highest band, '" + below.getGradeCode() + "', ends at "
                    + plain(below.getMaximumValue()) + ".";
        } else if (above != null) {
            between = " The lowest band, '" + above.getGradeCode() + "', starts at "
                    + plain(above.getMinimumValue()) + ".";
        } else {
            between = " This scheme has no bands at all.";
        }

        throw ApiException.notFound("GRADE_NOT_RESOLVABLE",
                "No band of this scheme covers " + plain(value) + "." + between
                        + " That is a gap in the scale, which #1 reports as a warning rather than "
                        + "refusing — a gap a school left on purpose is indistinguishable from one "
                        + "it did not mean.");
    }

    /**
     * Whether a whole mark can land in one span.
     *
     * <p>The inclusivity flags are not decoration. A leading gap starts at 0 and 0 is gradeable,
     * so it is closed at the bottom; a middle gap sits between two bands that each claim their own
     * boundary, so it is open at both ends; a trailing gap ends at the scheme's ceiling, which is
     * gradeable, so it is closed at the top. Treating all three the same would either invent a gap
     * at 0 on every scheme or miss an ungraded ceiling on the schemes that have one.
     */
    private static boolean containsWholeMark(BigDecimal low, boolean lowInclusive,
            BigDecimal high, boolean highInclusive) {

        BigDecimal candidate = low.setScale(0, RoundingMode.FLOOR);

        //! The first whole mark at or after `low`. Bumped when low is fractional (its floor is
        //! below the span) or when low itself is claimed by the band below.
        if (candidate.compareTo(low) < 0 || !lowInclusive) {
            candidate = candidate.add(BigDecimal.ONE);
        }

        int comparison = candidate.compareTo(high);
        return highInclusive ? comparison <= 0 : comparison < 0;
    }
}
