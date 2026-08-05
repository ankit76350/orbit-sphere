package com.orbitastra.backend.models.new_new.academics.curriculum;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.academics.enums.CurriculumStatus;
import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A versioned curriculum followed by the school, such as a CBSE, IB, Cambridge,
 * state-board, or internally designed curriculum.
 */
@Document(collection = "curriculum_frameworks")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_curriculum_code_version_uniq",
                def = "{'schoolId': 1, 'frameworkCode': 1, 'frameworkVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_curriculum_programme_status_idx",
                def = "{'schoolId': 1, 'affiliationProgrammeDocsId': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumFramework extends SchoolBase {

    // Optionally links to AffiliationProgramme.id for board curricula.
    // Example: "67aa15d9dc3f7d0011111111"
    private String affiliationProgrammeDocsId;

    // Example: "CBSE Secondary Curriculum"
    @NotBlank
    private String name;

    // Example: "2026.1"
    @NotBlank
    private String frameworkVersion;

    // Example: "Central Board of Secondary Education"
    private String sourceAuthority;

    // First AcademicYear.name using this framework. Example: "2026-2027"
    private String effectiveFromAcademicYear;

    // Last AcademicYear.name using this framework. Example: "2028-2029"
    private String effectiveUntilAcademicYear;

    // Example: CurriculumStatus.APPROVED
    @NotNull
    @Builder.Default
    private CurriculumStatus status = CurriculumStatus.DRAFT;
}
