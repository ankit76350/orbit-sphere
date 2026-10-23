package com.orbitastra.backend.models.procurement.enums;

/**
 * Where a request to buy something has got to.
 *
 * <p>The path is DRAFT to SUBMITTED to APPROVED, and then the ordering states as purchase
 * orders are raised against it.
 *
 * <p>PARTIALLY_ORDERED is a real state, not a tidy-up. A request for rice, oil and
 * vegetables may well be split across three vendors, and the first order being placed must
 * not make the request look finished. Without it, whoever raised the request has no way of
 * seeing that the vegetables were never ordered.
 *
 * <p>REJECTED keeps the request rather than deleting it, and it carries a reason. A
 * department that has asked for the same thing four times and been refused four times is
 * something a head should be able to see.
 */
public enum ProcurementRequestStatus {
    /** Being written. Not yet anybody else's business. */
    DRAFT,

    /** Sent for approval. */
    SUBMITTED,

    /** Approved, and waiting for purchase orders to be raised. */
    APPROVED,

    /** Refused, with a reason kept. */
    REJECTED,

    /** Some lines have been ordered and some have not. */
    PARTIALLY_ORDERED,

    /** Every line has been ordered. */
    ORDERED,

    /** Withdrawn by the department, or no longer needed. */
    CANCELLED
}
