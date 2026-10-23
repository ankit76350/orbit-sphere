package com.orbitastra.backend.models.finance.enums;

/**
 * Online payment provider a school has set up for collecting fees.
 *
 * <p>This is the company processing the payment. What the parent actually used,
 * such as UPI or a card, is stored separately as the payment mode.
 */
public enum GatewayProvider {
    /** Razorpay. */
    RAZORPAY,

    /** Cashfree Payments. */
    CASHFREE,

    /** Easebuzz. */
    EASEBUZZ,

    /** PayU. */
    PAYU,

    /** CCAvenue. */
    CCAVENUE,

    /** BillDesk. */
    BILLDESK,

    /** A provider not listed above. */
    OTHER
}
