package com.orbitastra.backend.models.academics.enums;

/**
 * Overall outcome recorded on one published ReportCard version.
 *
 * <p>This is the result decision only. Promotion to the next class is not stored
 * here; it closes the current StudentAcademicRecord and creates the next year's
 * record.
 */
public enum ResultStatus {
    /** Student met the passing requirement for the reporting period. */
    PASS,

    /** Student did not meet the passing requirement. */
    FAIL,

    /** Result is intentionally not disclosed, for example pending a review. */
    WITHHELD,

    /** Required marks or components are missing, so no outcome was determined. */
    INCOMPLETE
}
