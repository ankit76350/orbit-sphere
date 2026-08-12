package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How money physically reached the school.
 *
 * <p>This is the instrument used, not the state of the payment. A gateway
 * provider such as Razorpay is stored separately on the payment, because the
 * same provider can carry UPI, card and net-banking payments.
 */
public enum PaymentMode {
    /** Notes and coins collected at the counter. */
    CASH,

    /** Bank cheque handed over by the payer. */
    CHEQUE,

    /** Demand draft handed over by the payer. */
    DEMAND_DRAFT,

    /** Direct bank transfer such as NEFT, RTGS or IMPS. */
    BANK_TRANSFER,

    /** UPI transfer, including an automatic AutoPay debit. */
    UPI,

    /** Debit or credit card. */
    CARD,

    /** Net banking through a payment gateway. */
    NET_BANKING,

    /** Money already held in the student's wallet. */
    WALLET,

    /** No money moved; balances were adjusted in the books. */
    ADJUSTMENT
}
