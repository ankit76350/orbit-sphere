package com.orbitastra.backend.dto.academics.gradingscheme.request;

import java.math.BigDecimal;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.GradingScaleType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * Edits to one scheme. Endpoint #3.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A request that sends
 * nothing is a {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, so a client with a bug
 * in its form finds out.
 *
 * <h2>It can change everything — and mostly it cannot be used</h2>
 *
 * <p>The plan for this module said there would be <b>no {@code PATCH} at all</b>, on the grounds
 * that every field is either half the key, a reinterpretation of every band beneath it, the
 * history itself, or an event with its own endpoint. That reasoning was right about a scheme
 * <i>something has used</i> and wrong about one nothing has: a school that mistypes a boundary
 * during setup should not have to create version 2 to fix a typo in version 1.
 *
 * <p>So the rule moved from the <i>field</i> to the <i>state</i>:
 *
 * <pre>
 * nothing references this scheme   every field below is editable
 * something references it          409, whatever the body says
 * </pre>
 *
 * <p><b>{@code active} is not here — that is #4 and #5.</b> It carried briefly on 2026-09-14 and
 * was taken back off the same day: every other lifecycle flag in this project is a POST event with
 * a name — {@code /results/lock} on a term, {@code /enrollment/enable} on a year, eight of them
 * across two modules — and {@code AcademicTermUpdateRequest} refuses {@code active} in these exact
 * words: <i>"events with meanings, not fields to toggle in passing"</i>. A grading scheme is not
 * the one document that should differ.
 *
 * <p><b>It also simplifies the guard.</b> With {@code active} gone, every field this record carries
 * changes what a stored grade means — so the reference check is unconditional rather than asking
 * first whether the body touched grading at all.
 *
 * <h2>The band set is replaced whole, never patched</h2>
 *
 * <p>Send {@code gradeBands} and it replaces every band; leave it out and none change. There is no
 * way to edit one band, because a band set is only valid as a whole: adding or moving one always
 * risks an overlap or a gap with its neighbours, and the checks that catch those read the entire
 * set.
 *
 * <h2>What is refused, and by which check</h2>
 *
 * <p>Every rule #1 applies is re-applied here against the <b>resulting</b> scheme rather than the
 * body, which is what makes a half-change safe to send. Two are worth naming because they surprise
 * people:
 *
 * <ul>
 *   <li><b>Lowering {@code maximumValue}</b> below a band's top is {@code 409
 *       GRADE_BAND_OUTSIDE_SCALE} — the bands you did not send are still checked against the
 *       ceiling you did.</li>
 *   <li><b>Switching {@code scaleType} to {@code DESCRIPTOR}</b> while the stored bands still carry
 *       bounds is {@code 400 GRADE_BAND_BOUNDS_NOT_ALLOWED}. Send the new bands in the same
 *       request; the two fields are one change.</li>
 * </ul>
 */
public record GradingSchemeUpdateRequest(

        /**
         * A new name. Half the unique key, so this is a <b>rename of the rulebook</b> — every
         * other version keeping the old name stops looking related, and #9 splits one history in
         * two. Refused outright once anything references the scheme.
         */
        @Size(max = 120) String name,

        /** A new version string. The other half of the key; same uniqueness check. */
        @Size(max = 40) String schemeVersion,

        /**
         * A new scale.
         *
         * <p>It reinterprets every band beneath it, so changing it almost always means sending
         * {@code gradeBands} too — a percentage band has bounds and a descriptor band must not.
         */
        GradingScaleType scaleType,

        /** A new ceiling. Required by PERCENTAGE and MARKS, refused by DESCRIPTOR. */
        BigDecimal maximumValue,

        /**
         * A replacement band set — all of them, or none.
         *
         * <p>Capped at 40, the same as #1. An empty list is {@code 400 GRADE_BANDS_REQUIRED}: a
         * scheme that grades nothing is not a scheme, and clearing the bands is not a way to
         * retire one.
         */
        @Size(max = 40, message = "a scheme cannot have more than 40 bands")
        @Valid
        List<GradeBandRequest> gradeBands) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return name == null && schemeVersion == null && scaleType == null
                && maximumValue == null && gradeBands == null;
    }
}
