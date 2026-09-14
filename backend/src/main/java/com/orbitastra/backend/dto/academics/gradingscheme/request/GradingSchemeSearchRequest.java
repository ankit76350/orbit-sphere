package com.orbitastra.backend.dto.academics.gradingscheme.request;

import com.orbitastra.backend.models.academics.enums.GradingScaleType;

/**
 * What #6 filters the school's schemes by.
 *
 * <p><b>Every field is optional and absent means "do not filter on this".</b> Absent is not the
 * same as {@code false}: {@code ?active=} left off returns retired versions as well as live ones,
 * which is the difference between "show me everything" and "show me what is retired".
 *
 * <p><b>The tenant is not here.</b> It comes from the header and is passed to the repository
 * separately, because it is the boundary of what may be read rather than a filter a caller chose.
 * A filter a caller can send is one a caller can leave off.
 *
 * <p><b>And no academic year</b>, unlike {@code AcademicTermSearchRequest}. A rulebook outlives a
 * year — there is no year to scope this to, which is why the whole endpoint is a segment shorter
 * than every list in {@code academics/structure}.
 *
 * <p><b>Three filters and no more.</b> There is deliberately nothing for "schemes with a gap" or
 * "schemes used by a subject": the first is recomputed per read rather than stored, so filtering
 * on it would mean computing it for every row in the collection; the second is a query across
 * three collections that [open item 4] has not settled.
 */
public record GradingSchemeSearchRequest(

        /** Schemes offered for new work, or retired ones. Absent returns both. */
        Boolean active,

        /**
         * PERCENTAGE · POINT · DESCRIPTOR.
         *
         * <p>The one filter a caller uses to answer a real question: "what can I grade an exam
         * out of 100 with" excludes descriptor schemes, which cannot be resolved by value at all.
         */
        GradingScaleType scaleType,

        /**
         * Case-insensitive, matches anywhere in {@code name}. Blank is treated as absent.
         *
         * <p><b>{@code name} only</b>, unlike the term search which also matches a code. There is
         * no code on a scheme to match — the consequence of keying on {@code name}, and an
         * argument for the {@code schemeCode} that open item 3 keeps on the table.
         */
        String search,

        Integer page,
        Integer size,
        String sort) {
}
