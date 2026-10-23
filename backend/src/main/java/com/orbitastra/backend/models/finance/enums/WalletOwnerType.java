package com.orbitastra.backend.models.finance.enums;

/**
 * Who a StoredValueAccount belongs to.
 */
public enum WalletOwnerType {
    /** The account belongs to one student. */
    STUDENT,

    /** The account belongs to one guardian and may cover several children. */
    GUARDIAN,

    /** The account belongs to one staff member. */
    STAFF
}
