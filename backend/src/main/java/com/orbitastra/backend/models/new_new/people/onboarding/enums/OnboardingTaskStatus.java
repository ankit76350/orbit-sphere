package com.orbitastra.backend.models.new_new.people.onboarding.enums;

/**
 * State of one checklist task inside an onboarding case.
 */
public enum OnboardingTaskStatus {
    /** Task is waiting to be started. */
    PENDING,

    /** Task is currently being handled. */
    IN_PROGRESS,

    /** Task cannot continue until an issue is resolved. */
    BLOCKED,

    /** Task was completed. */
    COMPLETED,

    /** Authorized staff decided the task was not required. */
    WAIVED
}
