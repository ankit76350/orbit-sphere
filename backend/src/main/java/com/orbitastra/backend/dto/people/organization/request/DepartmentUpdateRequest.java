package com.orbitastra.backend.dto.people.organization.request;

import jakarta.validation.constraints.Size;

/**
 * Edits to one org unit. Endpoint #10.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A request that sends
 * nothing is a {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, so a client with a bug
 * in its form finds out.
 *
 * <h2>What this deliberately cannot change</h2>
 *
 * <p><b>Not {@code departmentCode}.</b> Nothing joins on it, which is exactly what makes editing
 * it dangerous: no query would break, and every export, filter and report naming the old code
 * would quietly stop matching. The {@code termCode} reasoning, unchanged.
 *
 * <p><b>Not {@code parentDepartmentDocsId} — a unit cannot be moved.</b> The plan had #10 moving
 * one and refusing a cycle at {@code 409 DEPARTMENT_CYCLE}; that was dropped on 2026-09-15. Where
 * a unit sits is decided when it is created, under the parent whose page created it, and a move
 * is the one edit here that changes what every OTHER unit's page shows. Nesting stays a create.
 *
 * <p>The consequence is worth stating plainly: <b>no endpoint in this product can write a cycle
 * into the department chart.</b> #9 cannot, because a new unit has no children; #10 cannot,
 * because it does not accept a parent at all. #12's tree builder still carries its visited set —
 * that is what makes the sentence above true of the data rather than merely of the code.
 *
 * <h2>active is here, and #11 is why that needs saying</h2>
 *
 * <p>The plan gave {@code active} its own endpoint pair — {@code POST /departments/{id}/deactivate}
 * and {@code /reactivate}, #11 — the shape every lifecycle flag in this project uses. Putting the
 * field here instead was asked for on 2026-09-15, and it is a real departure.
 *
 * <p><b>It carries #11's refusal with it.</b> Retiring a unit that still holds active seats is a
 * {@code 409 DEPARTMENT_NOT_EMPTY} naming how many, exactly as the plan specified — because the
 * rule belongs to the transition, not to the endpoint that happens to perform it. A field that
 * reached the same state without the check would be a back door around a decision this module
 * already made.
 *
 * <h2>What can be cleared, and what cannot</h2>
 *
 * <pre>
 * "description": ""       clears it
 * "headStaffDocsId": ""   clears it — the unit has no named head
 * "name": ""              400 DEPARTMENT_NAME_REQUIRED
 * any field: null         leaves it            (same as absent)
 * </pre>
 */
public record DepartmentUpdateRequest(

        /** A new display name. Blank is refused, not treated as a clear. */
        @Size(max = 120) String name,

        /** New free text, or {@code ""} to remove what it has. */
        @Size(max = 500) String description,

        /**
         * A new head's {@code Staff.id}, or {@code ""} to leave the unit without one.
         *
         * <p>Validated to <b>exist</b>, not to be employed — the same rule #9 follows, and for the
         * same reason: a school enters its org chart before its employment records.
         */
        @Size(max = 60) String headStaffDocsId,

        /**
         * Retire the unit, or restore it.
         *
         * <p>{@code false} is refused while active seats remain. {@code true} has no such check
         * and needs none — restoring a unit cannot invalidate anything.
         */
        Boolean active) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return name == null && description == null && headStaffDocsId == null && active == null;
    }
}
