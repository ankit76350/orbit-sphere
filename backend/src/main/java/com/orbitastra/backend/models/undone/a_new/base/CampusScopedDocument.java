package com.orbitastra.backend.models.undone.a_new.base;

import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base for data owned by one operational campus.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class CampusScopedDocument extends TenantScopedDocument {

    @Indexed
    @NotBlank
    private String campusDocsId;

    private String legalEntityDocsId;
}
