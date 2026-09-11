package com.orbitastra.backend.services.academics.helper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;

import lombok.RequiredArgsConstructor;

/**
 * The academics module's single helper — the rules MongoDB cannot express about terms.
 *
 * <p>The module plan lists three containment rules that are service checks and nothing else: a
 * term's dates fall inside the year, terms do not overlap, and {@code sequence} is unique. They
 * are here because #1, #2, #3, #4 and #35 all need them and five copies would eventually disagree.
 *
 * <p><b>Every method takes the year's terms rather than reading them.</b> A year holds two to four
 * terms, so the service loads the set once and passes it in — which also means every check answers
 * against the same snapshot. A helper that queried for itself would make five round trips and
 * could see five different states.
 *
 * <p><b>Each method stands on its own and calls nothing else in this class</b>, per the service
 * code-writing rules. The one thing two of them would share — "do these ranges overlap" — lives in
 * {@link Dates#overlaps} in {@code common}, where core's academic-year check reads it too.
 *
 * <p><b>Checks throw rather than return a flag.</b> One that returned false would leave every
 * caller to invent its own message and status code. The exception is
 * {@link #weightSumWarning(List)}, which deliberately does not throw — see its note.
 */
@Component
@RequiredArgsConstructor
public class AcademicsHelper {

    //! terms — used by endpoints 1 to 4 and 35 ----------------------------------------

    /**
     * A term's last day cannot precede its first.
     *
     * <p>Equal dates are legal: a one-day term is odd but not wrong, and refusing it would be
     * inventing a rule the model does not state.
     *
     * Used by:
     * - createTerm()
     */
    public void validateTermRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw ApiException.badRequest("INVALID_TERM_RANGE",
                    "A term ending " + Dates.readable(end) + " cannot start on "
                            + Dates.readable(start) + ".");
        }
    }

    /**
     * A term must fall inside the academic year that owns it.
     *
     * <p>The year is the only thing that says which dates belong to it — a term reaching past it
     * would be reported in a year whose calendar does not cover those days, and every "what is
     * happening this term" query would disagree with every "what is happening this year" one.
     *
     * Used by:
     * - createTerm()
     */
    public void validateTermWithinYear(LocalDate start, LocalDate end,
            LocalDate yearStart, LocalDate yearEnd) {

        if (start.isBefore(yearStart) || end.isAfter(yearEnd)) {
            throw ApiException.conflict("TERM_OUTSIDE_ACADEMIC_YEAR",
                    Dates.readable(start) + " to " + Dates.readable(end) + " is outside "
                            + Dates.readable(yearStart) + " to " + Dates.readable(yearEnd)
                            + ", which is what this academic year covers.");
        }
    }

    /**
     * No two ACTIVE terms of one year may cover the same day.
     *
     * <p><b>Retired terms are ignored on purpose</b>, unlike the code and sequence checks below.
     * A term that is no longer taught should not block the dates it used to hold, and
     * {@code school_year_term_active_dates_idx} is indexed on {@code active} for exactly this
     * query. The code and the sequence stay taken because their unique indexes do not filter on
     * {@code active} — three rules, two different answers to "does a retired term still count",
     * and the difference is deliberate.
     *
     * <p>Overlap itself is {@link Dates#overlaps}, so this and core's academic-year check cannot
     * come to disagree about touching endpoints.
     *
     * @param ignoreId the term being edited, excluded so it does not overlap itself
     *
     * Used by:
     * - createTerm()
     */
    public void validateNoTermOverlap(List<AcademicTerm> yearTerms, String ignoreId,
            LocalDate start, LocalDate end) {

        for (AcademicTerm other : yearTerms) {
            if (other.getId().equals(ignoreId) || !Boolean.TRUE.equals(other.getActive())) {
                continue;
            }
            if (Dates.overlaps(start, end, other.getStartDate(), other.getEndDate())) {
                throw ApiException.conflict("TERMS_OVERLAP",
                        "These dates cover days '" + other.getName() + "' already covers ("
                                + Dates.readable(other.getStartDate()) + " to "
                                + Dates.readable(other.getEndDate())
                                + "). Two terms cannot cover the same day.");
            }
        }
    }

    /**
     * A {@code termCode} is unique within one school's year.
     *
     * <p><b>Retired terms still hold theirs.</b> {@code school_year_term_code_uniq} does not
     * filter on {@code active}, so a check that skipped retired rows would accept a write the
     * database then refuses with a duplicate key — a 500 where a 409 was meant.
     *
     * @param ignoreId the term being edited, excluded so it does not clash with itself
     *
     * Used by:
     * - createTerm()
     */
    public void validateTermCodeFree(List<AcademicTerm> yearTerms, String ignoreId,
            String termCode) {

        for (AcademicTerm other : yearTerms) {
            if (other.getId().equals(ignoreId)) {
                continue;
            }
            if (termCode.equalsIgnoreCase(other.getTermCode())) {
                throw ApiException.conflict("TERM_CODE_TAKEN",
                        "This year already has a term coded " + other.getTermCode() + " ('"
                                + other.getName() + "')"
                                + (Boolean.TRUE.equals(other.getActive()) ? "" : ", retired")
                                + ". A code stays taken once used, because records store it.");
            }
        }
    }

    /**
     * A {@code sequence} is unique within one school's year.
     *
     * <p><b>Retired terms still hold theirs</b>, for the same reason as the code: the unique
     * index does not filter on {@code active}.
     *
     * @param ignoreId the term being edited, excluded so it does not clash with itself
     *
     * Used by:
     * - createTerm()
     */
    public void validateSequenceFree(List<AcademicTerm> yearTerms, String ignoreId,
            Integer sequence) {

        for (AcademicTerm other : yearTerms) {
            if (other.getId().equals(ignoreId)) {
                continue;
            }
            if (sequence.equals(other.getSequence())) {
                throw ApiException.conflict("TERM_SEQUENCE_TAKEN",
                        "'" + other.getName() + "' is already sequence " + sequence
                                + " in this year"
                                + (Boolean.TRUE.equals(other.getActive()) ? "" : " (retired)")
                                + ".");
            }
        }
    }

    /**
     * A year either weights every active term or none of them.
     *
     * <p><b>This is refused, while a wrong SUM is only reported</b> — the two are not the same
     * problem. A sum can be transiently wrong on the way from 20/80 to 30/70, which is why open
     * item 3 says a single-term write must not refuse one. A mixture has no such transition:
     * there is no sequence of edits where one term is weighted and another is not that ends
     * somewhere valid, and the annual result cannot be computed from it at all.
     *
     * <p>Only active terms are considered — a retired term's weight is not part of any annual
     * result.
     *
     * Used by:
     * - createTerm()
     */
    public void validateWeightNotMixed(List<AcademicTerm> yearTerms, BigDecimal weightPercent) {
        List<AcademicTerm> active = yearTerms.stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .toList();

        if (active.isEmpty()) {
            return;
        }

        boolean othersWeighted = active.stream().anyMatch(t -> t.getWeightPercent() != null);
        boolean othersUnweighted = active.stream().anyMatch(t -> t.getWeightPercent() == null);

        if (weightPercent == null && othersWeighted) {
            throw ApiException.conflict("TERM_WEIGHT_MIXED",
                    "The other terms in this year carry a weight, so this one needs one too. "
                            + "An annual result cannot be computed when some terms are weighted "
                            + "and others are not.");
        }

        if (weightPercent != null && othersUnweighted) {
            throw ApiException.conflict("TERM_WEIGHT_MIXED",
                    "No other term in this year carries a weight. Either weight them all or "
                            + "leave weightPercent out — a mixture computes to nothing.");
        }
    }

    /**
     * What to say when the active weights do not total 100.
     *
     * <p><b>Returns a message instead of throwing, and that is the whole point.</b> Two terms at
     * 20 and 80 become 30 and 70 in two calls, and the first is always transiently wrong —
     * 30 + 80 = 110. Refusing it would make the values impossible to change. So the sum is
     * checked wherever the whole set is written (#2, #4) and merely reported everywhere else,
     * which is what open item 3 settled.
     *
     * @return the warning, or null when the weights are fine or unused
     *
     * Used by:
     * - createTerm()
     */
    public String weightSumWarning(List<AcademicTerm> yearTerms) {
        List<AcademicTerm> weighted = yearTerms.stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()) && t.getWeightPercent() != null)
                .toList();

        if (weighted.isEmpty()) {
            return null;
        }

        BigDecimal total = weighted.stream()
                .map(AcademicTerm::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(new BigDecimal("100")) == 0) {
            return null;
        }

        return "The active term weights now total " + total.stripTrailingZeros().toPlainString()
                + "%, not 100%. The annual result cannot be computed until they do — this is a "
                + "warning rather than a refusal, because getting from one valid set to another "
                + "passes through an invalid one.";
    }
}
