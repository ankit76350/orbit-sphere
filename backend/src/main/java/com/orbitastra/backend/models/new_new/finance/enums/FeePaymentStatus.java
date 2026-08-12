package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * State of one collected or attempted FeePayment.
 *
 * <p>Only a SUCCEEDED payment may be spread across invoices. A payment is never
 * edited back to an earlier state once it has succeeded; a mistake is corrected
 * with a RefundTransaction or by reversing the allocations.
 */
public enum FeePaymentStatus {
    /** Started, usually by opening a payment gateway page. */
    INITIATED,

    /** Waiting for the bank or gateway to confirm, such as a cheque in clearing. */
    PENDING,

    /** Money has been received by the school. */
    SUCCEEDED,

    /** The attempt did not go through. */
    FAILED,

    /** Called off before any money moved. */
    CANCELLED,

    /** Money was taken back, such as a bounced cheque or a gateway chargeback. */
    REVERSED
}
