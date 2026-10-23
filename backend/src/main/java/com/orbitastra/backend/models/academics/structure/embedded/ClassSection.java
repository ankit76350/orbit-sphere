package com.orbitastra.backend.models.academics.structure.embedded;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One section embedded in its owning SchoolClass document.
 *
 * <p>{@code sectionNo} is the only section identifier. It is both the stable
 * reference other documents store and the value shown in the UI, so no separate
 * display name is kept. It must be unique inside the owning class and must not
 * be renamed after dependent records exist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSection {

    // Stable reference within the class; also the display value. Example: "A"
    @NotBlank
    private String sectionNo;

    // Optionally links to the class teacher's Staff.id.
    // Example: "67aa15d9dc3f7d0011111111"
    private String classTeacherDocsId;

    // Maximum planned student count. Example: 40
    private Integer capacity;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
