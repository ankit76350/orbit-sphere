package com.orbitastra.backend.models.undone.a_new.learning;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "learning_session_artifacts")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_virtual_artifact_type_version_uniq",
                def = "{'tenantId':1,'virtualLearningSessionDocsId':1,'artifactType':1,'versionNo':1}",
                unique = true),
        @CompoundIndex(name = "tenant_artifact_retention_idx",
                def = "{'tenantId':1,'retentionExpiresAt':1,'status':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningSessionArtifact extends TenantScopedDocument {

    private String virtualLearningSessionDocsId;
    private String artifactType;
    private Integer versionNo;
    private String storedObjectDocsId;
    private String generatedByAiRunDocsId;
    private String languageCode;
    private String status;
    private String approvedByDocsId;
    private Instant availableAt;
    private Instant retentionExpiresAt;
}
