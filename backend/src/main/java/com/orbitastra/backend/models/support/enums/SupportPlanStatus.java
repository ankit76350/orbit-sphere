package com.orbitastra.backend.models.new_new.support.enums;

/**
 * Whether a plan is being followed.
 *
 * <p>UNDER_REVIEW exists because a plan nobody looks at again is a plan nobody follows. A child
 * changes over a term, and an accommodation that helped in April may be holding them back by
 * December. The review is the point at which somebody asks whether it is still right.
 *
 * <p>DISCONTINUED is kept apart from COMPLETED. Completed means the child no longer needs it.
 * Discontinued means it stopped for another reason: the family declined, the specialist left,
 * the school could not staff it. Those are different, and only one of them is good news.
 */
public enum SupportPlanStatus {
    /** Being written. Not in force. */
    DRAFT,

    /** In force. Teachers are expected to follow it. */
    ACTIVE,

    /** Due to be looked at again, or being looked at now. */
    UNDER_REVIEW,

    /** Finished because the child no longer needs it. */
    COMPLETED,

    /** Stopped for some other reason, which is recorded. */
    DISCONTINUED
}
