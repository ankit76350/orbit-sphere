package com.orbitastra.backend.models.new_new.base;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.Version;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Common MongoDB identity, audit, and optimistic-locking metadata for every
 * top-level document in the new model design.
 *
 * <p>Instants are used for audit timestamps so values remain unambiguous across
 * school time zones. Spring Data fills the annotated audit properties when
 * MongoDB auditing and an {@code AuditorAware<String>} are configured.
 * {@code version} is managed by Spring Data and prevents one writer from
 * silently overwriting another writer's newer changes.
 *
 * <p>Embedded value objects must not extend this class because they do not own a
 * MongoDB collection identity or independent audit lifecycle.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditedDocument {

    // MongoDB _id. Example: "67aa15d9dc3f7d0098765432"
    @Id
    private String id;

    // Set once when the document is created. Example: 2026-07-30T08:30:00Z
    @CreatedDate
    private Instant createdAt;

    // Updated whenever the document changes. Example: 2026-07-31T10:15:00Z
    @LastModifiedDate
    private Instant updatedAt;

    // Links to the identity/account that created the document. Example: "67aa15d9dc3f7d0011111111"
    @CreatedBy
    private String createdByDocsId;

    // Links to the identity/account that last changed the document. Example: "67aa15d9dc3f7d0022222222"
    @LastModifiedBy
    private String updatedByDocsId;

    // Spring-managed optimistic-lock value. Example: 4
    @Version
    private Long version;
}
