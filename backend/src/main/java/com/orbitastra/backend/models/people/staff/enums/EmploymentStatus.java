package com.orbitastra.backend.models.people.staff.enums;

/**
 * Business lifecycle of one {@code EmploymentRecord}.
 *
 * <p><b>Each value carries its own two rules</b> rather than leaving them in an {@code if} chain
 * somewhere: whether changing to it needs a reason written down, and whether it ends the
 * employment. A status added later brings its rules with it, and nothing has to remember to
 * update a list in the service.
 *
 * <p><b>{@code OFFERED} was removed on 2026-09-16.</b> It meant "accepted an offer, has not
 * started", and the module plan's open item 2 made it the case {@code ?employed=true} exists to
 * exclude. That state now has no representation: somebody is employed here from the day their
 * record begins, and a future {@code effectiveFrom} is how a not-yet-started hire is expressed.
 * <b>#7's filter becomes "not terminal" rather than "not in {OFFERED, TERMINATED}".</b>
 */
public enum EmploymentStatus {

    /** Working, inside a probation period. A normal start, so no reason is needed. */
    PROBATION(false, false),

    /** Working. The ordinary state, and no reason is needed to reach it. */
    ACTIVE(false, false),

    /**
     * Away, and coming back — maternity, sabbatical, long illness.
     *
     * <p>A reason is required because "on leave" alone answers none of the questions a school
     * asks next: how long, whether to backfill, whether they are paid.
     */
    ON_LEAVE(true, false),

    /** Barred from working while something is looked into. A reason is not optional here. */
    SUSPENDED(true, false),

    /** Leaving, but still working out their notice. A reason says whose decision it was. */
    NOTICE_PERIOD(true, false),

    /** Employment ended by the school or by resignation. Terminal, and needs a reason. */
    TERMINATED(true, true),

    /** Employment ended by retirement. Terminal, and kept apart from termination on purpose. */
    RETIRED(true, true);

    private final boolean requiresReason;
    private final boolean terminal;

    EmploymentStatus(boolean requiresReason, boolean terminal) {
        this.requiresReason = requiresReason;
        this.terminal = terminal;
    }

    /**
     * Whether moving to this status has to be explained.
     *
     * <p>The two that do not are the two that need no explaining: somebody is working, or working
     * through a probation period. Every other value is a school answering "what happened".
     */
    public boolean requiresReason() {
        return requiresReason;
    }

    /**
     * Whether this status ends the employment.
     *
     * <p><b>A terminal status and {@code current = true} cannot both be true</b> — that is the
     * contradiction the module plan's open item 2 warns about, and nothing in the model prevents
     * it. Whatever sets a terminal status must close the record in the same write.
     */
    public boolean isTerminal() {
        return terminal;
    }
}
