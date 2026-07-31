package com.orbitastra.backend.models.new_new.people.onboarding.enums;

/**
 * Overall state of a new staff member's onboarding case.
 */
public enum OnboardingStatus {
    /** Onboarding work has not begun. */
    NOT_STARTED,

    /** One or more onboarding tasks are being processed. */
    IN_PROGRESS,

    /** Onboarding cannot continue until an issue is resolved. */
    BLOCKED,

    /** All required onboarding work is complete. */
    COMPLETED,

    /** Onboarding was cancelled. */
    CANCELLED
}
