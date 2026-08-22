package com.orbitastra.backend.models.common.enums;

/**
 * How a person was identified at the moment something was recorded about them.
 *
 * <p>It sits in common because the same question comes up wherever the school notes
 * that somebody was somewhere: at a gate, on a bus, in the mess queue. None of those
 * owns the idea, and three copies of it would drift apart.
 *
 * <p>The reason it is worth recording at all is that the answers are not equally
 * trustworthy. A card tap happened at a moment the system saw for itself. MANUAL is
 * somebody typing a name, which is fine but is a person's word. When a parent argues
 * about what time their child left, this is the difference between evidence and a
 * recollection.
 */
public enum IdentificationMethod {
    /** An ID card was scanned. */
    ID_CARD_SCAN,

    /** An ID card chip was tapped on a reader. */
    RFID_TAP,

    /** A printed code was scanned, such as on a visitor badge. */
    QR_SCAN,

    /** A face recognition camera recorded it. */
    FACE_RECOGNITION,

    /** A member of staff entered it by hand. */
    MANUAL
}
