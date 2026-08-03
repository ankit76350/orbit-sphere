package com.orbitastra.backend.models.new_new.academics.structure.embedded;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One section embedded in its owning SchoolClass document. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSection {

    // Example: "Section A - Pioneers"
    @NotBlank
    private String name;

    // Optionally links to the class teacher's Staff.id.
    // Example: "67aa15d9dc3f7d0011111111"
    private String classTeacherDocsId;

    // Maximum planned student count. Example: 40
    private Integer capacity;

    // Optionally links to a future facility/resource document.
    //! Example: "67aa15d9dc3f7d0022222222"
    private String roomResourceDocsId;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
