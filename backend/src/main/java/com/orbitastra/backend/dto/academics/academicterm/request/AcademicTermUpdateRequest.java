package com.orbitastra.backend.dto.academics.academicterm.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Edits to one term. Endpoint #3.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A request that sends
 * nothing is a {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, so a client with a bug
 * in its form finds out.
 *
 * <h2>What this deliberately cannot change</h2>
 *
 * <p><b>Not {@code termCode}.</b> Six documents across three modules point at this term by
 * {@code termDocsId} — so a code edit would not orphan them, which is exactly what makes it
 * dangerous: nothing would fail and nothing would cascade, while every school-facing report,
 * export and saved filter naming the old code would quietly stop matching. The code is the half
 * of a term that is meant to outlive everything, which is why #1 makes a school state it outright
 * rather than deriving it from a name that <i>is</i> editable.
 *
 * <p><b>Not {@code sequence} — that is #4.</b> It is unique within the year, so swapping two
 * terms one {@code PATCH} at a time hits {@code school_year_term_sequence_uniq} halfway through:
 * term 1 becomes 2 while term 2 is still 2. A reorder has to be one write that sees every term,
 * and that is a different endpoint rather than a field here.
 *
 * <p><b>Not {@code active} and not {@code resultsLocked}</b> — #5 to #8. Retiring a term and
 * freezing its results are events with meanings, not fields to toggle in passing.
 *
 * <h2>What can be cleared, and what cannot</h2>
 *
 * <pre>
 * "name": ""                 400 TERM_NAME_REQUIRED
 * any field: null            leaves it            (same as absent)
 * </pre>
 *
 * <p><b>There is no way to clear {@code weightPercent} here, and that is not an oversight.</b> A
 * year weights every active term or none of them, so removing one term's weight is only ever
 * legal as part of removing them all — which is #2, the one endpoint that writes every row at
 * once. A clear on this endpoint would be refused by the mixture rule in every year that has more
 * than one active term, so offering it would be offering a field that almost never works.
 *
 * <h2>The weight sum is reported, never refused</h2>
 *
 * <p>Two terms at 20 and 80 become 30 and 70 in two calls, and the first call is always
 * transiently wrong — 30 + 80 = 110. Refusing that would make the values impossible to change, so
 * a broken total comes back as {@code warning} on a successful response. This is the difference
 * from #1, which <i>can</i> refuse an excess because a create only ever adds to the sum, and from
 * #2, which can require exactly 100 because it sees every row at once.
 */
public record AcademicTermUpdateRequest(

        /** A new display name. Blank is refused, not treated as a clear. */
        @Size(max = 120) String name,

        /** A new first day, inclusive. Checked against the year and the other active terms. */
        LocalDate startDate,

        /** A new last day, inclusive. Sending one date alone keeps the other as it is. */
        LocalDate endDate,

        /** A new share of the annual result. Cannot be cleared — see the class note. */
        @DecimalMin("0") @DecimalMax("100") BigDecimal weightPercent) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return name == null && startDate == null && endDate == null && weightPercent == null;
    }

    /** Whether anything here moves the term in the calendar, which is what needs the year read. */
    public boolean touchesDates() {
        return startDate != null || endDate != null;
    }
}
