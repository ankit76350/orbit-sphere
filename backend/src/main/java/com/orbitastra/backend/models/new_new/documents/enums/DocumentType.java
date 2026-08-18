package com.orbitastra.backend.models.new_new.documents.enums;

/**
 * What kind of paper is being asked for or issued.
 *
 * <p>ID cards are not in this list. They are their own model, because a card
 * carries a photo, an expiry, an RFID number and a history of replacements, and
 * none of that fits a certificate.
 */
public enum DocumentType {
    /** Confirms the student studies here. The most asked-for paper in a school. */
    BONAFIDE_CERTIFICATE,

    /** Given when a student leaves, so another school can admit them. */
    TRANSFER_CERTIFICATE,

    /** Speaks to the student's conduct while they were here. */
    CHARACTER_CERTIFICATE,

    /** Confirms which classes were studied and passed, over which years. */
    STUDY_CERTIFICATE,

    /** A statement of marks for one exam or one year. */
    MARKSHEET,

    /** Confirms a staff member worked here, in what job, for how long. */
    EXPERIENCE_LETTER,

    /** States a staff member's pay, usually for a bank or a visa. */
    SALARY_CERTIFICATE,

    /** Given to a staff member on their last day. */
    RELIEVING_LETTER,

    /** Anything the school words for itself. The template says what it is. */
    CUSTOM
}
