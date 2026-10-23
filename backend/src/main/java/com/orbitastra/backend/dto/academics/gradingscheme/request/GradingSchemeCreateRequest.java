package com.orbitastra.backend.dto.academics.gradingscheme.request;

import java.math.BigDecimal;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.GradingScaleType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A rulebook and its bands, in one write. Endpoint #1.
 *
 * <h2>The scale decides the shape of everything else</h2>
 *
 * <p>{@code scaleType} is read before any band is looked at, because it says whether bands carry
 * bounds at all:
 *
 * <pre>
 * PERCENTAGE   maximumValue required, 100            every band bounded
 * MARKS        maximumValue required, the total      every band bounded
 * DESCRIPTOR   maximumValue refused                  no band bounded
 * </pre>
 *
 * <p><b>The scale names what a teacher enters, never what comes out.</b> The awarded grade is
 * a band's {@code gradeCode} and its weight in an aggregate is {@code gradePoint}; both are
 * mapped <i>from</i> a value on one of these scales. An IB scheme is {@code PERCENTAGE} or
 * {@code MARKS} with bands coded "7" down to "1" — the 1–7 is the grade IB awards, not the
 * number a teacher types.
 *
 * <p>Checking a band's bounds before knowing the scale would produce the right refusal for the
 * wrong reason — "this band needs a minimum" when the real answer is "this scheme does not
 * measure anything".
 *
 * <h2>name + schemeVersion is the key</h2>
 *
 * <p>Unique together within one school, and <b>neither can be changed afterwards</b>: there is no
 * rename endpoint and there must not be one while {@code school_grading_name_version_uniq} indexes
 * {@code name}. The two versions of one rulebook are found by carrying the identical name, so a
 * rename would split one history into two — silently, and only #9 would notice.
 *
 * <h2>What is not here</h2>
 *
 * <p><b>No {@code active}.</b> It starts {@code true} and is an event with its own endpoints (#4,
 * #5). A scheme created already retired is a state nothing asked for — the same rule every create
 * in {@code academics} follows.
 *
 * <p><b>No {@code academicYear}</b>, unlike every other document in this module. A rulebook
 * outlives a year: the same scheme grades 2026-2027 and 2027-2028, and a report card from either
 * must reprint identically years later. What moves when the rules move is {@code schemeVersion}.
 */
public record GradingSchemeCreateRequest(

        /**
         * The rulebook's name — "CBSE Percentage Grading".
         *
         * <p>Half the unique key, so it identifies rather than labels. Every version of one
         * rulebook must carry it identically.
         */
        @NotBlank @Size(max = 120) String name,

        /**
         * Which version of that rulebook — "2026.1".
         *
         * <p>Free text, ordered by the school's own convention. It moves when the <i>rules</i>
         * move, not when the calendar does.
         */
        @NotBlank @Size(max = 40) String schemeVersion,

        /** PERCENTAGE · MARKS · DESCRIPTOR. Never changes — it reinterprets every band under it. */
        @NotNull GradingScaleType scaleType,

        /**
         * The ceiling the bands are read against — 100 for a percentage, the paper total for marks.
         *
         * <p>Required for PERCENTAGE and MARKS, refused for DESCRIPTOR. Optional here because the
         * rule depends on {@code scaleType}, which bean validation cannot reach.
         */
        BigDecimal maximumValue,

        /**
         * Every band, in the order they should print.
         *
         * <p><b>Stored in the order given, never re-sorted.</b> A school listing A1 first means
         * A1 first, and a response that silently reordered them would make a typo hard to spot
         * against the paper it was copied from.
         *
         * <p>Capped at 40 because the longest real scale — a 100-point descriptor ladder — is
         * well under it, and a cap stops one request loading an unbounded list into memory.
         */
        @NotEmpty(message = "a scheme needs at least one grade band")
        @Size(max = 40, message = "a scheme cannot have more than 40 bands")
        @Valid
        List<GradeBandRequest> gradeBands) {
}
