package com.orbitastra.backend.models.documents.enums;

/**
 * Who asked for a document.
 *
 * <p>A parent may ask from the portal, a student may ask for themself, and the
 * front office may raise the same ask for a family that walked in. All three end
 * up as the same record, so this says which happened and which collection
 * {@code requestedByDocsId} points at.
 */
public enum DocumentRequesterType {
    /** A parent or another guardian asked, usually from the parent portal. */
    GUARDIAN,

    /** A student asked for themself. */
    STUDENT,

    /** A staff member asked, either for themself or for a family. */
    STAFF
}
