package com.orbitastra.backend.models.new_new.plans.billing.enums;

/**
 * Financial lifecycle of a SubscriptionInvoice.
 */
public enum SubscriptionInvoiceStatus {
    /** Invoice is being prepared and is not yet legally issued. */
    DRAFT,

    /** Invoice was issued and payment is due. */
    ISSUED,

    /** Some, but not all, outstanding value was paid. */
    PARTIALLY_PAID,

    /** Outstanding amount reached zero. */
    PAID,

    /** Due date passed while an amount remains outstanding. */
    OVERDUE,

    /** Invoice was cancelled without deleting the financial record. */
    VOID
}
