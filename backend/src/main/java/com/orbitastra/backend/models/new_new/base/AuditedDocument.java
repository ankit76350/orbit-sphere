package com.orbitastra.backend.models.new_new.base;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Target persistence metadata for new aggregate roots.
 *
 * <p>Instants are used for audit timestamps so values remain unambiguous across
 * tenant time zones. {@code @Version} supplies optimistic concurrency control;
 * APIs must require the current version for mutable workflow resources.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditedDocument {

    @Id
    private String id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private String createdByDocsId;

    private String updatedByDocsId;

    //For optimistic-locking error
    @Version
    private Long version;
}
