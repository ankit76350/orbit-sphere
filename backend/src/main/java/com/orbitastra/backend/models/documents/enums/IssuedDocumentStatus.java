package com.orbitastra.backend.models.new_new.documents.enums;

/**
 * Whether an issued document still stands.
 *
 * <p>This is what a public check answers. Somebody holding a printed certificate
 * types its code and finds out whether the school still stands behind it.
 *
 * <p>SUPERSEDED and REVOKED are different on purpose. SUPERSEDED means a corrected
 * copy was issued and the newer one is the real one. REVOKED means the school has
 * taken it back, usually because it should never have been given.
 */
public enum IssuedDocumentStatus {
    /** Issued and still good. */
    VALID,

    /** A corrected copy was issued in its place. */
    SUPERSEDED,

    /** Taken back by the school, with a reason kept on the record. */
    REVOKED,

    /** Ran past the date it was good until. */
    EXPIRED
}
