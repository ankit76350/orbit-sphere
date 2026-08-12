package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How far an approved concession reaches.
 *
 * <p>This is what tells a standing discount apart from a one-off. A standing
 * discount is granted once and used again on every eligible bill inside its
 * validity dates, so the family never has to ask a second time. A one-off is
 * tied to a single bill and is finished the moment that bill is worked out.
 *
 * <p>Invoice generation only looks at ACADEMIC_YEAR requests. An INVOICE request
 * is applied by the invoice it names and by nothing else, which is what stops a
 * one-off favour from quietly becoming a year-long discount.
 */
public enum ConcessionScope {
    /** Applies to every eligible invoice between validFrom and validUntil. */
    ACADEMIC_YEAR,

    /** Applies only to the one invoice named on the request. */
    INVOICE
}
