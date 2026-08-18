package com.orbitastra.backend.models.new_new.conduct.enums;

/**
 * Whether what was decided actually happened.
 *
 * <p>NOT_COMPLETED exists because a detention nobody served is the normal way a
 * discipline system quietly stops working. If the only states were pending and done,
 * a skipped action would sit as pending forever and look like a queue rather than a
 * failure.
 */
public enum ConductActionStatus {
    /** Decided, not started. */
    PENDING,

    /** Being carried out, such as a suspension part-way through. */
    IN_PROGRESS,

    /** Done. */
    COMPLETED,

    /** Should have happened and did not, with the reason recorded. */
    NOT_COMPLETED,

    /** Called off, with the reason recorded. */
    CANCELLED
}
