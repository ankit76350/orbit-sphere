package com.orbitastra.backend.models.new_new.documents.enums;

/**
 * Whether a template may be used to make new documents.
 *
 * <p>SUPERSEDED and RETIRED both mean "not for new documents", and neither
 * touches the papers already issued from it. A template is never edited once it
 * has been used; a change makes the next version instead.
 */
public enum DocumentTemplateStatus {
    /** Being written, and not yet usable. */
    DRAFT,

    /** In use for new documents. */
    ACTIVE,

    /** Replaced by a newer version of the same template. */
    SUPERSEDED,

    /** Withdrawn, with no replacement. */
    RETIRED
}
