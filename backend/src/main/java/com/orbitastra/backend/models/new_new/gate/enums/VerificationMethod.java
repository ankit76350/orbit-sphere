package com.orbitastra.backend.models.new_new.gate.enums;

/**
 * How the person at the gate was identified.
 *
 * <p>Kept because the answers are not equally trustworthy. A card tap happened at
 * a moment the system saw for itself. MANUAL is a guard typing a name, which is
 * fine but is somebody's word. When a parent argues about what time their child
 * left, this field says how much the record is worth.
 */
public enum VerificationMethod {
    /** An ID card was scanned. */
    ID_CARD_SCAN,

    /** An ID card chip was tapped on a reader. */
    RFID_TAP,

    /** A visitor pass was scanned. */
    VISITOR_PASS_SCAN,

    /** A face recognition camera recorded it. */
    FACE_RECOGNITION,

    /** A guard entered it by hand. */
    MANUAL
}
