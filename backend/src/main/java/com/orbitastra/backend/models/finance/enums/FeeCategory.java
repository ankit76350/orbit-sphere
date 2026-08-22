package com.orbitastra.backend.models.finance.enums;

/**
 * Broad grouping of what a FeeHead charges for.
 *
 * <p>This is only for grouping and reporting. The amount, how often it is
 * charged and the tax rate all live on the FeeHead itself.
 */
public enum FeeCategory {
    /** Regular teaching fee. */
    TUITION,

    /** One-time charge collected when a student joins. */
    ADMISSION,

    /** Charge for conducting exams. */
    EXAMINATION,

    /** School bus or van charge. */
    TRANSPORT,

    /** Boarding charge for hostel students. */
    HOSTEL,

    /** Food charge for mess or canteen service. */
    MESS,

    /** Library membership charge. */
    LIBRARY,

    /** Science or computer lab charge. */
    LABORATORY,

    /** Sports, clubs, trips and other activity charges. */
    ACTIVITY,

    /** Uniform, books and other items sold by the school. */
    MATERIAL,

    /** Refundable money held by the school, such as a caution deposit. */
    DEPOSIT,

    /** Extra charge added when a fee is paid after its due date. */
    LATE_FEE,

    /** Penalty such as a damaged book or a lost ID card. */
    FINE,

    /** Anything the categories above do not cover. */
    OTHER
}
