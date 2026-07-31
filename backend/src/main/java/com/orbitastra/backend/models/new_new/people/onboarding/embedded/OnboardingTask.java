package com.orbitastra.backend.models.new_new.people.onboarding.embedded;

import java.time.Instant;
import java.time.LocalDate;

import com.orbitastra.backend.models.new_new.people.onboarding.enums.OnboardingTaskStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Checklist task embedded in an OnboardingCase.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingTask {

    // Stable key within the onboarding case. Example: "VERIFY_CREDENTIALS"
    @NotBlank
    private String taskKey;

    // Example: "Verify educational certificates"
    @NotBlank
    private String title;

    // Links to the assigned Staff or identity account.
    private String assigneeDocsId;

    // Example: 2026-08-25
    private LocalDate dueDate;

    // Example: OnboardingTaskStatus.IN_PROGRESS
    @NotNull
    @Builder.Default
    private OnboardingTaskStatus status = OnboardingTaskStatus.PENDING;

    // Links to supporting completion evidence.
    private String evidenceDocumentDocsId;

    // Example: 2026-08-24T11:00:00Z
    private Instant completedAt;
}
