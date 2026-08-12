package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * The accounting nature of a LedgerAccount.
 *
 * <p>The type decides which side of a journal entry increases the account and
 * which report the account appears on.
 */
public enum LedgerAccountType {
    /** Something the school owns or is owed, such as a bank balance or fees due. */
    ASSET,

    /** Something the school owes, such as fees collected in advance. */
    LIABILITY,

    /** The school's own funds and accumulated surplus. */
    EQUITY,

    /** Money the school earns, such as tuition income. */
    REVENUE,

    /** Money the school spends, such as salaries or electricity. */
    EXPENSE
}
