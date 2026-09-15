package com.orbitastra.backend.dto.people.organization.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A new approved seat inside a department. Endpoint #13.
 *
 * <h2>There is no code</h2>
 *
 * <p>{@code positionCode} was removed on 2026-09-15. A seat is addressed by its document id, which
 * is what {@code EmploymentRecord.positionDocsId} already stores — so {@code title} is what a
 * person reads, and what must be unique inside its department.
 *
 * <h2>What is not here</h2>
 *
 * <p><b>No {@code active}.</b> A seat starts {@code true}. Retiring it is #14, and unlike a
 * department that is a field on the patch rather than its own endpoint — a position has no
 * dependents to check, so the event has no rules of its own to enforce.
 *
 * <h2>approvedHeadcount follows the model, not the plan</h2>
 *
 * <p>The plan says "defaults to 1; null means uncapped". <b>The model says {@code @NotNull} with a
 * builder default of 1</b>, so "uncapped" is not a state a stored position can be in. This record
 * follows the model: absent means 1, and a null or a zero is refused. Making uncapped real means
 * dropping {@code @NotNull} from {@code Position}, which is a model change and not this
 * endpoint's to make.
 */
public record PositionCreateRequest(

        /** What the seat is called, and unique within its department. "Mathematics Teacher". */
        @NotBlank @Size(max = 120) String title,

        /** The owning unit, which must be one of this school's and must be <b>active</b>. */
        @NotBlank @Size(max = 60) String departmentDocsId,

        /**
         * The seat this one reports to, or null.
         *
         * <p><b>Not required to be in the same department.</b> A school with one Head of
         * Safeguarding that every department reports to on that line is a real structure — the
         * reporting line and the org tree answer different questions and are not kept consistent.
         */
        @Size(max = 60) String reportsToPositionDocsId,

        /** Approved seats of this kind. Absent means 1; below 1 is refused. */
        @Min(1) Integer approvedHeadcount,

        /**
         * Whether somebody in this seat teaches.
         *
         * <p><b>Absent means false, and it should almost always be sent.</b> It is what a teacher
         * picker filters on, so a school that leaves it false everywhere gets an empty picker with
         * nothing to explain it — which is why the response warns when a department's seats are
         * all non-teaching.
         */
        Boolean teachingPosition) {
}
