package com.orbitastra.backend.models.procurement.enums;

/**
 * Whether money actually reached the vendor.
 *
 * <p>INITIATED and COMPLETED are separate because a bank transfer is not instant and a
 * cheque is slower still. A cheque written on Monday and cleared on Thursday is money the
 * school has committed but not yet lost, and the vendor has been promised but not yet
 * received. Treating the two as one moment means the school's own bank figure is wrong for
 * three days.
 *
 * <p>FAILED and CANCELLED are both endings and they mean different things. FAILED is the
 * bank refusing: the transfer bounced, the cheque was returned. CANCELLED is the school
 * changing its mind before the money went. A failure needs somebody to try again; a
 * cancellation does not.
 */
public enum VendorPaymentStatus {
    /** Sent on its way. Cheque written, transfer submitted. Not yet cleared. */
    INITIATED,

    /** The money reached the vendor. */
    COMPLETED,

    /** The bank refused it. Somebody has to try again. */
    FAILED,

    /** Stopped before the money left. */
    CANCELLED
}
