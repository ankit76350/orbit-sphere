package com.orbitastra.backend.models.finance.enums;

/**
 * What a StoredValueAccount holds money for.
 *
 * <p>One owner may hold one account of each type, so pocket money kept for a
 * child does not get spent on fees by mistake.
 */
public enum WalletAccountType {
    /** Money paid in advance to be used against future fee invoices. */
    FEE_ADVANCE,

    /** Spending money held for the student, such as for the canteen. */
    POCKET_MONEY,

    /** Money kept for mess or canteen charges only. */
    MESS,

    /** Refundable money held against library borrowing. */
    LIBRARY_DEPOSIT,

    /** Refundable caution money held while the student is enrolled. */
    SECURITY_DEPOSIT
}
