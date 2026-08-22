package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * Money lifecycle of one FeeInvoice.
 *
 * <p>An invoice that has been issued is a financial record. A mistake is fixed
 * by making a reversing invoice or by voiding it, never by editing the amounts
 * or deleting the document.
 */
public enum FeeInvoiceStatus {
    /** Being prepared and not yet given to the parent. */
    DRAFT,

    /** Given to the parent and payment is now due. */
    ISSUED,

    /** Part of the amount has been paid. */
    PARTIALLY_PAID,

    /** Nothing is left to pay. */
    PAID,

    /** The due date has passed and money is still owed. */
    OVERDUE,

    /** The school has given up on collecting the remaining amount. */
    WRITTEN_OFF,

    /** Cancelled without deleting the record. */
    VOID,

    /** Cancelled by a matching reversing invoice. */
    REVERSED
}
