package com.orbitastra.backend.models.undone.a_new.base;

import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base for year/programme-specific operational and academic records.
 *
 * <p>References use immutable document ids. Human labels such as "2026-2027"
 * are presentation data and must never be foreign keys.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AcademicScopedDocument extends CampusScopedDocument {

    @Indexed
    @NotBlank
    private String academicYearDocsId;

    private String programmeDocsId;
}
