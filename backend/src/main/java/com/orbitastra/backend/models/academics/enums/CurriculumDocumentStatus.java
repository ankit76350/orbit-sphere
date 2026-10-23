package com.orbitastra.backend.models.academics.enums;

/** Publishing lifecycle of one uploaded curriculum document. */
public enum CurriculumDocumentStatus {
    /** Department is still preparing or reviewing the document. */
    DRAFT,

    /** Document is approved and visible to its intended users. */
    PUBLISHED,

    /** Older document retained only for history. */
    ARCHIVED
}
