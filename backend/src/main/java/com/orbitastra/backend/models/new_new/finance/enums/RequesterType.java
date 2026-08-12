package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * Who raised a finance request that somebody else has to decide.
 *
 * <p>A parent may ask for a discount from the parent portal, and the fee desk may
 * raise the same ask for a family that walked in. Both end up as the same record,
 * so this field is what says which of the two happened and which collection
 * {@code requestedByDocsId} points at.
 *
 * <p>The rule that the person who raised a request cannot also approve it is
 * checked by the service. A GUARDIAN can never approve anything.
 */
public enum RequesterType {
    /** A parent or another guardian asked, usually from the parent portal. */
    GUARDIAN,

    /** A staff member asked, either on their own or for the family. */
    STAFF
}
