package com.orbitastra.backend.models.finance.enums;

/**
 * Publication state of one version of a FeeStructure.
 *
 * <p>Only an ACTIVE version may be used to create invoices. Changing the
 * amounts of a version that has already produced invoices is not allowed; the
 * school creates a new version instead, and the old one becomes SUPERSEDED.
 */
public enum FeeStructureStatus {
    /** Still being prepared and cannot be used yet. */
    DRAFT,

    /** Sent for approval and waiting for a decision. */
    PENDING_APPROVAL,

    /** Approved and in use for creating invoices. */
    ACTIVE,

    /** Replaced by a newer version of the same structure. */
    SUPERSEDED,

    /** No longer offered and kept only for old invoices. */
    RETIRED
}
