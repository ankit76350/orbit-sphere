package com.orbitastra.backend.models.academics.enums;

/**
 * Whether a child on an exam's candidate list may sit its papers.
 *
 * <p>Only an {@code ELIGIBLE} candidate appears on a paper's hall register. A {@code DEBARRED}
 * one does not — their name is left off, so no attendance can be taken for them — until they
 * are let back in.
 */
public enum ExamCandidateStatus {
    /** May sit the exam. Their name is on the hall register of every paper they take. */
    ELIGIBLE,

    /** Stopped from sitting, with a reason — for example fees not paid. Not on any hall register. */
    DEBARRED
}
