package com.orbitastra.backend.models.new_new.people.onboarding;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.onboarding.embedded.OnboardingTask;
import com.orbitastra.backend.models.new_new.people.onboarding.enums.OnboardingStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Tracks onboarding work after a Staff profile and EmploymentRecord are created.
 *
 * <p>The recruitment link is optional so schools can onboard employees hired
 * outside the recruitment module.
 */
@Document(collection = "staff_onboarding_cases")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_staff_onboarding_joining_uniq",
                def = "{'schoolId': 1, 'staffDocsId': 1, 'joiningDate': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_onboarding_status_joining_idx",
                def = "{'schoolId': 1, 'status': 1, 'joiningDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCase extends SchoolBase {

    // Optionally links to RecruitmentApplication.id.
    private String recruitmentApplicationDocsId;

    // Links to Staff.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String staffDocsId;

    // Links to EmploymentRecord.id. Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String employmentRecordDocsId;

    // Example: 2026-09-01
    @NotNull
    private LocalDate joiningDate;

    // Links to the onboarding owner's Staff.id.
    // Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String ownerDocsId;

    // Example: OnboardingStatus.IN_PROGRESS
    @NotNull
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.NOT_STARTED;

    // Example: 2027-03-01
    private LocalDate probationReviewDate;

    // Embedded onboarding checklist.
    @Builder.Default
    private List<OnboardingTask> tasks = new ArrayList<>();
}
