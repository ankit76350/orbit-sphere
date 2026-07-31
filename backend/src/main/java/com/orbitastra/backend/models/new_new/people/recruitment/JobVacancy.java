package com.orbitastra.backend.models.new_new.people.recruitment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.people.recruitment.enums.JobVacancyStatus;
import com.orbitastra.backend.models.new_new.people.staff.enums.StaffCredentialType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Approved hiring requirement for one Position.
 *
 * <p>Applications link to this document through {@code vacancyDocsId}. The
 * filled count is derived from hired RecruitmentApplication records and is not
 * stored here.
 */
@Document(collection = "staff_job_vacancies")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_vacancy_no_uniq",
                def = "{'schoolId': 1, 'vacancyNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_vacancy_status_close_idx",
                def = "{'schoolId': 1, 'status': 1, 'closesAt': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JobVacancy extends SchoolBase {

    // Generated using NumberSequenceType.JOB_VACANCY.
    // Example: "VAC/2026/000001"
    @NotBlank
    private String vacancyNo;

    // Links to Position.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String positionDocsId;

    // Number of employees to hire. Example: 2
    @NotNull
    private Integer openings;

    // Example: "Two additional teachers are required for increased enrollment."
    private String justification;

    // Example: JobVacancyStatus.OPEN
    @NotNull
    @Builder.Default
    private JobVacancyStatus status = JobVacancyStatus.DRAFT;

    // Example: 2026-08-01T00:00:00Z
    private Instant opensAt;

    // Example: 2026-08-31T23:59:59Z
    private Instant closesAt;

    // Links to the responsible recruiter's Staff.id.
    // Example: "67aa15d9dc3f7d0022222222"
    private String recruiterDocsId;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean publiclyVisible = false;

    // Example: ["CLASSROOM_MANAGEMENT", "MATHEMATICS"]
    @Builder.Default
    private List<String> requiredSkillCodes = new ArrayList<>();

    // Example: [StaffCredentialType.EDUCATIONAL_QUALIFICATION]
    @Builder.Default
    private List<StaffCredentialType> requiredCredentialTypes = new ArrayList<>();
}
