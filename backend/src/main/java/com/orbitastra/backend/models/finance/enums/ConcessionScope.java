package com.orbitastra.backend.models.new_new.finance.enums;

/**
 * How far an approved discount reaches.
 *
 * <p>This is what tells a year-long discount apart from a one-off. A year-long
 * discount is approved once and used again on every matching bill inside its
 * dates, so the family never has to ask a second time. A one-off belongs to one
 * bill and is finished as soon as that bill is worked out.
 *
 * <p>The billing job only looks at ACADEMIC_YEAR requests. An INVOICE request is
 * used by the one bill it names and by nothing else. That is what stops a one-off
 * favour from turning into a year-long discount by mistake.
 */
public enum ConcessionScope {
    /** Comes off every matching bill between validFrom and validUntil. */
    ACADEMIC_YEAR,

    /** Comes off only the one bill named on the request. */
    INVOICE
}
