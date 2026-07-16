package com.orbitastra.backend.models.crm.enums;

/**
 * Lifecycle of a formal admission (application) — the accept/reject/enroll
 * decision, distinct from the InquiryStatus funnel and the enrolled StudentStatus.
 */
public enum AdmissionStatus {

    /** Application submitted, awaiting the school's review decision. Default on creation. */
    PENDING,

    /** Accepted by the school, but not yet enrolled (e.g. awaiting documents, fee, or a seat). */
    APPROVED,

    /** Declined — the application will not proceed. Captured nowhere else in the system. */
    REJECTED,

    /** Enrolled — converted into a Student (set together with the admission's studentId). */
    CONFIRMED
}
