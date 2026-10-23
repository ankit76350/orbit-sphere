package com.orbitastra.backend.models.finance.enums;

/**
 * Why money is being sent back to the payer.
 *
 * <p>This is a fixed list so refunds can be counted and reviewed. Free-text
 * detail goes in the remarks field on the refund itself.
 */
public enum RefundReason {
    /** The same fee was paid twice. */
    DUPLICATE_PAYMENT,

    /** More was paid than the invoice asked for. */
    OVERPAYMENT,

    /** The student left the school. */
    WITHDRAWAL,

    /** A concession or scholarship was approved after the fee was paid. */
    CONCESSION_APPLIED_LATE,

    /** A paid service, such as transport or a trip, was not used. */
    SERVICE_NOT_AVAILED,

    /** A refundable deposit is being returned. */
    DEPOSIT_RETURN,

    /** The school billed the wrong amount. */
    BILLING_ERROR,

    /** The payment gateway or the bank made an error. */
    GATEWAY_ERROR,

    /** Anything the reasons above do not cover. */
    OTHER
}
