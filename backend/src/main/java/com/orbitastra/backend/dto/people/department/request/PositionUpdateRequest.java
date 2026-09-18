package com.orbitastra.backend.dto.people.department.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Edits to one approved seat. Endpoint #14.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A request that sends
 * nothing is a {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, so a client with a bug
 * in its form finds out.
 *
 * <h2>What this deliberately cannot change</h2>
 *
 * <p><b>Not {@code departmentDocsId} — a seat cannot move department.</b> Editing it in place
 * would rewrite where every past holder worked, and every employment record under the seat would
 * silently change department with it. A seat in another unit is a new seat. This is also what
 * keeps {@code school_department_title_uniq} meaningful: a title is unique <i>within</i> a unit,
 * and a seat that could move carries its title across that boundary.
 *
 * <p>It is the same call #10 makes about a department's parent, made for a different reason: a
 * department's parent is structure, a seat's department is history.
 *
 * <h2>The reporting line can cycle, and this is the endpoint that can write one</h2>
 *
 * <p>#13 needs no cycle walk — a brand-new seat has nothing reporting to it. This one can move an
 * existing seat under its own subordinate, which is exactly the case the module plan's open item 2
 * describes: a chain that closes on itself is a stack overflow in whatever first walks it, months
 * later and in a different module. {@code 409 POSITION_CYCLE}.
 *
 * <h2>What can be cleared, and what cannot</h2>
 *
 * <pre>
 * "reportsToPositionDocsId": ""   clears it — the seat reports to nobody
 * "title": ""                     400 POSITION_TITLE_REQUIRED
 * "approvedHeadcount": 0          400 — the model forbids it, and null is not "uncapped"
 * any field: null                 leaves it            (same as absent)
 * </pre>
 */
public record PositionUpdateRequest(

        /** A new title. Blank is refused, not treated as a clear. Unique within the department. */
        @Size(max = 120) String title,

        /**
         * A new supervising seat, or {@code ""} to report to nobody.
         *
         * <p>Not required to be in the same department — a school with one Head of Safeguarding
         * that every unit reports to on that line is a real structure. The org tree and the
         * reporting line answer different questions.
         */
        @Size(max = 60) String reportsToPositionDocsId,

        /**
         * A new approved headcount, at least 1.
         *
         * <p><b>Null is not "uncapped".</b> {@code Position} declares this {@code @NotNull} with a
         * builder default of 1, so uncapped is not a state a stored seat can be in — absent here
         * means "leave it alone", and making uncapped real would be a model change.
         */
        @Min(1) Integer approvedHeadcount,

        /** Whether somebody in this seat teaches. It is what a teacher picker filters on. */
        Boolean teachingPosition,

        /** Retire the seat, or restore it. A retired seat keeps its title. */
        Boolean active) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return title == null && reportsToPositionDocsId == null && approvedHeadcount == null
                && teachingPosition == null && active == null;
    }
}
