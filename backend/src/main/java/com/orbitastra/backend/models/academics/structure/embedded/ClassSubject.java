package com.orbitastra.backend.models.academics.structure.embedded;

import java.util.ArrayList;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.SubjectType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One subject assignment embedded in its owning SchoolClass document.
 *
 * <p>{@code sectionNo} is null for a class-wide assignment. Repeat the same
 * {@code subjectCode} with different section codes when teachers differ by
 * section.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSubject {

    // Stable reference within the class. Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Example: "Mathematics"
    @NotBlank
    private String name;

    // Example: "Maths"
    private String shortName;

    // Example: SubjectType.CORE
    @NotNull
    private SubjectType subjectType;

    // Optional ClassSection.sectionNo; null means all sections. Example: "A"
    private String sectionNo;

    // Links to assigned Staff.id values.
    // Example: ["67aa15d9dc3f7d0011111111"]
    @Builder.Default
    private List<String> teacherDocsIds = new ArrayList<>();

    // Optionally links to GradingScheme.id.
    //! Example: "67aa15d9dc3f7d0022222222"
    private String gradingSchemeDocsId;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
