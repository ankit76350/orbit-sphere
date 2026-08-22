package com.orbitastra.backend.models.common.enums;

/**
 * How money changed hands.
 *
 * <p>Used both ways round. A family paying fees and a school paying a salary reach for the
 * same short list, which is why this sits in common rather than under finance: neither
 * direction owns the idea.
 *
 * <p>CASH matters more than it looks. Support staff at many schools are still paid in cash,
 * and a system that assumed every payment was a bank transfer would have nowhere to record
 * it, which means no record of the payment at all.
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
