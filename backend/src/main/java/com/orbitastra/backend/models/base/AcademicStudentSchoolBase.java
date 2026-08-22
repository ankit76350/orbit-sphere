package com.orbitastra.backend.models.base;

import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for school-owned collection documents that belong to one student
 * in one academic year.
 *
 * <p>{@code academicYear} stores the immutable {@code AcademicYear.name}, such
 * as {@code "2026-2027"}; it never stores the AcademicYear document id.
 * {@code studentDocsId} references {@code Student.id}. Concrete collections
 * should normally add compound indexes beginning with {@code schoolId} and
 * matching their own access patterns instead of relying only on these
 * single-field indexes.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AcademicStudentSchoolBase extends SchoolBase {

    // Links to AcademicYear.name. Example: "2026-2027"
    @Indexed
    @NotBlank
    private String academicYear;

    // Links to Student.id. Example: "67aa15d9dc3f7d0055555555"
    @Indexed
    @NotBlank
    private String studentDocsId;
}
