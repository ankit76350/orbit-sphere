package com.orbitastra.backend.models.undone.a_new.base;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;

import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.RecordState;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base for every customer-owned document in the target SaaS model.
 *
 * <p>{@code tenantId} is the mandatory isolation and future shard key. It is
 * intentionally not named {@code schoolId}: one subscribed tenant may operate
 * a group containing several legal entities and campuses.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class TenantScopedDocument extends AuditedDocument {

    @Indexed
    @NotBlank
    private String tenantId;

    @Builder.Default
    private RecordState recordState = RecordState.ACTIVE;

    private Instant archivedAt;

    private Instant deletedAt;

    private String deletedByDocsId;
}
