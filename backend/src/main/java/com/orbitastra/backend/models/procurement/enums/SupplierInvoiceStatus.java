package com.orbitastra.backend.models.procurement.enums;

/**
 * Where a vendor's bill has got to on its way to being paid.
 *
 * <p>VERIFIED and APPROVED are two different people doing two different jobs, which is why
 * they are two states. Verifying is clerical: does this bill match what was ordered and
 * what actually arrived, at the rates agreed? Approving is authority: yes, pay it. One
 * state for both means the person who checks the arithmetic is also the person who releases
 * the money, and that is the arrangement every audit asks about.
 *
 * <p>DISPUTED is the important one. A bill the school is refusing to pay is not overdue,
 * and it is not paid either. Without its own state it sits in the payables list looking
 * like a bill somebody forgot, ages quietly, and turns up in a report as money the school
 * owes when in fact the school has said it does not.
 */
public enum SupplierInvoiceStatus {
    /** The bill has arrived and been entered. Nobody has checked it yet. */
    RECEIVED,

    /** Checked against the order and the delivery. The figures are right. */
    VERIFIED,

    /** Cleared for payment by somebody with the authority to say so. */
    APPROVED,

    /** Some of it has been paid. */
    PARTIALLY_PAID,

    /** Paid in full. */
    PAID,

    /** The school is refusing to pay, and the reason is recorded. */
    DISPUTED,

    /** Withdrawn, or entered by mistake. */
    CANCELLED
}
