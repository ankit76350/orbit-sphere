package com.orbitastra.backend.models.crm.enums;

/**
 * Stage of a lead in the admissions funnel — the "where in the pipeline" marker,
 * distinct from the per-touchpoint notes captured in the follow-up timeline. The
 * inquiry's top-level status mirrors its latest follow-up entry.
 */
public enum InquiryStatus {

    /** Fresh lead — someone enquired but no engagement yet. Default on creation. */
    INQUIRY,

    /** Being worked by a counselor (calls, guidance, discussions). */
    COUNSELING,

    /** Prospect visited the campus — a key milestone in the funnel. */
    VISIT,

    /** An admission (application) has been created for this lead. Set automatically when the admission is made. */
    ADMITTED,

    /** The linked admission was converted into an enrolled Student. Set automatically. */
    CONFIRMED,

    /** Dropped — the lead will not proceed (not interested / chose elsewhere). Terminal. */
    LOST
}
