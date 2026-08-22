package com.orbitastra.backend.models.documents.enums;

/**
 * Who a document is about.
 *
 * <p>This is the person named on the paper, which is not always the person who
 * asked for it. A parent asks for a bonafide certificate; the certificate is about
 * their child.
 */
public enum DocumentHolderType {
    /** The document is about a student. Points at Student.id. */
    STUDENT,

    /** The document is about a member of staff. Points at Staff.id. */
    STAFF,

    /** The document is about a parent or guardian. Points at Guardian.id. */
    GUARDIAN
}
