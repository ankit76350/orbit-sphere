package com.orbitastra.backend.models.conduct.enums;

/**
 * How far one child's case has got.
 *
 * <p>REFERRED is the one to watch. It means the case has gone beyond ordinary
 * discipline, usually to safeguarding or to an outside agency, and it must not sit in
 * a discipline queue as though somebody is still deciding a detention.
 */
public enum ConductCaseStatus {
    /** Recorded, nobody has looked at it yet. */
    OPEN,

    /** Somebody is finding out what happened. */
    UNDER_REVIEW,

    /** Decided, and the actions are still being carried out. */
    ACTION_PENDING,

    /** Finished, with an outcome recorded. */
    CLOSED,

    /** Handed on because it is more than a discipline matter. */
    REFERRED,

    /** Withdrawn, because it turned out not to have happened. */
    WITHDRAWN
}
