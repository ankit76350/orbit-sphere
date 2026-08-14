package com.orbitastra.backend.models.new_new.transport.enums;

/**
 * How a boarding was recorded.
 *
 * <p>Kept because the answers are not equally trustworthy. A card tap happened at
 * a moment the system saw for itself. MANUAL is somebody remembering, possibly
 * later, and a parent disputing a record deserves to know which of the two it was.
 */
public enum BoardingCaptureMethod {
    /** The attendant or driver marked it by hand in the app. */
    MANUAL,

    /** The student tapped an RFID card on the reader. */
    RFID_CARD,

    /** A QR code on the student's card was scanned. */
    QR_SCAN,

    /** A face recognition device on the bus recorded it. */
    FACE_RECOGNITION
}
