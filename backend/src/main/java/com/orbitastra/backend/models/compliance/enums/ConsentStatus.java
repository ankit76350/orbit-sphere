package com.orbitastra.backend.models.compliance.enums;

/**
 * Whether the school may act on a consent today.
 *
 * <p>Only GRANTED allows anything. The other three are all ways of not being allowed, and
 * they are kept apart because they need different action: PENDING means ask the family,
 * WITHDRAWN means stop and do not ask again, EXPIRED means ask again.
 *
 * <p>A withdrawal never deletes the record. A school asked why it published a photograph in
 * March has to be able to show the consent that stood in March, and deleting it on
 * withdrawal in April would remove exactly that.
 */
public enum ConsentStatus {
    /** Asked for and not answered. The school may not act. */
    PENDING,

    /** Given. The school may act. */
    GRANTED,

    /** Refused outright. */
    REFUSED,

    /** Given and later taken back. Stop, and do not act again. */
    WITHDRAWN,

    /** Ran past the date it was good for. Ask again. */
    EXPIRED
}
