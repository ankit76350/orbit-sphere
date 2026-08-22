package com.orbitastra.backend.models.finance.enums;

/**
 * Whether a PaymentAllocation still counts.
 *
 * <p>An allocation is never deleted. Undoing one means writing a new reversing
 * allocation and marking this one REVERSED, so the history of how a payment was
 * spread across invoices stays readable.
 */
public enum AllocationStatus {
    /** Counts towards the invoice's paid amount. */
    ACTIVE,

    /** Cancelled by a matching reversing allocation. */
    REVERSED,

    /** Written only to cancel an earlier allocation. */
    REVERSAL
}
