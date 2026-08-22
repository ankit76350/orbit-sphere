package com.orbitastra.backend.models.audit.enums;

/**
 * Whether it worked.
 *
 * <p>Failures and refusals are recorded, not only successes. A trail that holds only what
 * succeeded cannot answer the two questions most often asked of it: did somebody try to do
 * this and get stopped, and did this keep going wrong.
 *
 * <p>DENIED and FAILURE are different. DENIED is the system working: somebody asked for
 * something they may not have. FAILURE is the system not working: they were allowed and it
 * broke anyway. Counting them together hides both.
 */
public enum AuditOutcome {
    /** It happened. */
    SUCCESS,

    /** They were allowed to, and it broke. */
    FAILURE,

    /** They were not allowed to. The system working as intended. */
    DENIED
}
