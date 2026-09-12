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
 * <p><b>No check here calls another check here</b>, per the service code-writing rules. The one
 * thing two of them would share — "do these ranges overlap" — lives in {@link Dates#overlaps} in
 * {@code common}, where core's academic-year check reads it too. The single exception is
 * {@code plain}, which formats a number for a message and decides nothing; it is private and
 * stayed here rather than going to {@code common} because a display formatter is not the job
 * {@code TextHelper} describes for itself.
 *
 * <p><b>Checks throw rather than return a flag.</b> One that returned false would leave every
 * caller to invent its own message and status code. The exception is
 * {@link #weightSumWarning(List)}, which deliberately does not throw — see its note.
 */
@Component
@RequiredArgsConstructor
public class AcademicsHelper {

    /** A year is 100% of itself. Compared against, never added to. */
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /** 20.00 reads as "20" in a message, and 20 stays "20". */
    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

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
     * A term {@code name} is unique within one school's year.
     *
     * <p><b>Added 2026-09-12, and it is the rule a class already had.</b> Two terms both called
     * "Term 1" leave every dropdown, every report-card header and every screen showing the same
     * period twice with nothing to tell them apart — and a report card names the term rather than
     * its code, so the ambiguity lands in front of parents rather than staying inside the
     * database.
     *
     * <p><b>Case-folded, unlike the class check it mirrors.</b> "Term 1" and "term 1" are one
     * name: a year holding both is a data-entry mistake in every school, never a structure
     * anybody designed. That makes this check <i>stricter</i> than
     * {@code school_year_term_name_uniq}, which is case-sensitive like every MongoDB index
     * without a collation — the safe direction, since the service can never accept a write the
     * index then refuses.
     *
     * <p><b>Retired terms still hold theirs</b>, exactly as with the code and the sequence: the
     * index does not filter on {@code active}, so a check that skipped retired rows would accept
     * a write the database refuses with a duplicate key — a 500 where a 409 was meant.
     *
     * @param ignoreId the term being edited, excluded so it does not clash with itself
     *
     * Used by:
     * - createTerm()
     * - updateTerm()
     */
    public void validateTermNameFree(List<AcademicTerm> yearTerms, String ignoreId, String name) {
        for (AcademicTerm other : yearTerms) {
            if (other.getId().equals(ignoreId)) {
                continue;
            }
            if (name.equalsIgnoreCase(other.getName())) {
                throw ApiException.conflict("TERM_NAME_TAKEN",
                        "This year already has a term called '" + other.getName() + "' ("
                                + other.getTermCode() + ")"
                                + (Boolean.TRUE.equals(other.getActive()) ? "" : ", retired")
                                + ". A report card names the term, not its code, so two cannot "
                                + "share a name.");
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
     * The active weights of one year may not add up to more than 100%.
     *
     * <p><b>This is a refusal where a shortfall is only a warning</b>, and the asymmetry is the
     * point. {@link #weightSumWarning(List)} does not throw because two terms going from 20/80 to
     * 30/70 are transiently at 110 after the first write — refusing that would make the values
     * impossible to change. <b>That argument is about editing, and an insert is not an edit.</b>
     * A create only ever adds to the sum, so there is no legitimate path through "over 100" on
     * this endpoint: a school that needs room for a new term lowers an existing one first, with
     * #3, and then adds.
     *
     * <p><b>Only the ceiling is enforced, never equality.</b> Requiring exactly 100 here would
     * make the first weighted term of a year impossible to create — at 40%, the year totals 40,
     * and there would be no way to reach a second term. A year on its way to being weighted is
     * under 100 for as long as it takes to enter the terms, which is why that stays a warning.
     *
     * <p>Retired terms are ignored: their weight is not part of any annual result, so it cannot
     * consume room a live term needs.
     *
     * @param ignoreId the term being edited, excluded so its own weight is not counted twice
     *
     * Used by:
     * - createTerm()
     */
    public void validateWeightSumWithinLimit(List<AcademicTerm> yearTerms, String ignoreId,
            BigDecimal weightPercent) {

        if (weightPercent == null) {
            return;
        }

        BigDecimal alreadyUsed = yearTerms.stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()) && t.getWeightPercent() != null)
                .filter(t -> !t.getId().equals(ignoreId))
                .map(AcademicTerm::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = alreadyUsed.add(weightPercent);

        if (total.compareTo(ONE_HUNDRED) > 0) {
            BigDecimal remaining = ONE_HUNDRED.subtract(alreadyUsed);
            throw ApiException.conflict("TERM_WEIGHTS_EXCEED_100",
                    "The year's active terms already carry " + plain(alreadyUsed)
                            + "%, so a further " + plain(weightPercent) + "% would total "
                            + plain(total) + "%. A year is 100% of itself. "
                            + (remaining.signum() > 0
                                    ? "At most " + plain(remaining) + "% is left to give."
                                    : "There is nothing left to give — lower another term first, "
                                            + "which is what #3 is for."));
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
     * <p><b>This reports a shortfall; {@link #validateWeightSumWithinLimit} refuses an excess.</b>
     * Being under 100 is where every year on its way to being weighted sits, and it resolves
     * itself as the remaining terms are entered. Being over 100 on an insert does not resolve
     * itself: nothing a later create does brings the total back down.
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

        if (total.compareTo(ONE_HUNDRED) == 0) {
            return null;
        }

        return "The active term weights now total " + plain(total)
                + "%, not 100%. The annual result cannot be computed until they do — this is a "
                + "warning rather than a refusal, because getting from one valid set to another "
                + "passes through an invalid one.";
    }
}
