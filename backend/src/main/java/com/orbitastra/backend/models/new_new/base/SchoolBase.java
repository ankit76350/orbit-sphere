package com.orbitastra.backend.models.new_new.base;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;

import com.orbitastra.backend.models.new_new.base.enums.RecordState;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for every top-level MongoDB document owned by one school tenant.
 *
 * <p>{@code schoolId} is the tenant boundary. Repository and service queries
 * must always include it, even when querying by a globally unique MongoDB id.
 * This platform supports multiple school tenants but does not introduce a
 * separate campus boundary.
 *
 * <p>{@code recordState} implements recoverable lifecycle handling. Archiving
 * and soft deletion are service-level workflows; documents should not be
 * physically deleted during normal operations. Embedded value objects do not
 * extend this class because they inherit ownership and lifecycle from their
 * parent document.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class SchoolBase extends AuditedDocument {

    // Links to School.id and identifies the tenant. Example: "67aa15d9dc3f7d0033333333"
    @Indexed
    @NotBlank
    private String schoolId;

    // Current soft lifecycle state. Example: RecordState.ACTIVE
    @NotNull
    @Builder.Default
    private RecordState recordState = RecordState.ACTIVE;

    // Set when recordState becomes ARCHIVED. Example: 2026-08-01T09:00:00Z
    private Instant archivedAt;

    // Set when recordState becomes DELETED. Example: 2026-08-15T11:30:00Z
    private Instant deletedAt;

    // Links to the identity/account that soft-deleted the record. Example: "67aa15d9dc3f7d0044444444"
    private String deletedByDocsId;
}
