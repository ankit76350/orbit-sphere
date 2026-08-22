package com.orbitastra.backend.models.new_new.base.enums;

/**
 * Recoverable lifecycle state shared by school-owned documents.
 *
 * <p>This state is separate from business workflow statuses such as admission,
 * payment, or subscription status.
 */
public enum RecordState {
    /** Available for normal application use. */
    ACTIVE,

    /** Temporarily unavailable but not archived or deleted. */
    INACTIVE,

    /** Retained for history and normally excluded from active screens. */
    ARCHIVED,

    /** Soft-deleted and retained until the applicable retention policy permits removal. */
    DELETED
}
