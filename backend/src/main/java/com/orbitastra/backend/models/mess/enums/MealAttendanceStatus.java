package com.orbitastra.backend.models.new_new.mess.enums;

/**
 * Whether one person ate one meal.
 *
 * <p>The kitchen uses this to know how much to cook, not to work out a bill. Mess
 * charges are a fixed monthly amount on the hostel allocation, so a child who skips
 * breakfast is not refunded for it and a child who eats twice is not charged twice.
 *
 * <p>PACKED_TAKEN is kept apart from PRESENT because a child who took food away did eat
 * the school's food, but was not in the hall. The kitchen counts both; a warden looking
 * for who was in the hall counts only one.
 */
public enum MealAttendanceStatus {
    /** Ate in the hall. */
    PRESENT,

    /** Took food away, such as before an early match. */
    PACKED_TAKEN,

    /** Did not come. */
    ABSENT,

    /** Away on approved leave, so was never expected. */
    ON_LEAVE
}
