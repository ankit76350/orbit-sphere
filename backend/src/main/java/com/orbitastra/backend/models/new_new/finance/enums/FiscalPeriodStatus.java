package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * Whether journal entries may be posted into a FiscalPeriod.
 *
 * <p>The financial year of the books does not have to match the academic year,
 * which is why fiscal periods are their own collection.
 */
public enum FiscalPeriodStatus {
    /** Not started yet, so nothing can be posted into it. */
    FUTURE,

    /** Open for normal posting. */
    OPEN,

    /** Only accountants may still post, usually for closing entries. */
    SOFT_CLOSED,

    /** Locked, so nothing more can be posted. */
    CLOSED,

    /** Opened again by an approved request after being closed. */
    REOPENED
}
