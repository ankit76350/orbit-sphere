package com.orbitastra.backend.models.finance.enums;

/**
 * Who asked for a finance request that somebody else has to decide.
 *
 * <p>A parent can ask for a discount from the parent portal, and the fee desk can
 * also raise the same ask for a family that walks in. Both end up as the same
 * record, so this field says which of the two happened, and which collection
 * {@code requestedByDocsId} points at.
 *
 * <p>The service makes sure the person who asked is not also the person who
 * approves. A guardian can never approve anything.
 */
public enum RequesterType {
    /** A parent or another guardian asked, usually from the parent portal. */
    GUARDIAN,

    /** A staff member asked, either for themself or for the family. */
    STAFF
}
