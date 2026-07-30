package com.orbitastra.backend.models.new_new.base;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;

import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.RecordState;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Common persistence metadata shared by every top-level MongoDB document.
 * Embedded value objects do not extend this class because they do not own a
 * collection identity or tenant boundary.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class SchoolBase extends AuditedDocument {

    @Indexed
    @NotBlank
    private String schoolId;

    @Builder.Default
    private RecordState recordState = RecordState.ACTIVE;

    private Instant archivedAt;

    private Instant deletedAt;

    private String deletedByDocsId;
}
