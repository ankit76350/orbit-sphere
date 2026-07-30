package com.orbitastra.backend.models.undone.a_new.media;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "media_publication_decisions")
@CompoundIndex(name = "tenant_media_decision_version_uniq",
        def = "{'tenantId':1,'mediaItemDocsId':1,'decisionVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MediaPublicationDecision extends TenantScopedDocument {

    private String mediaItemDocsId;
    private Integer decisionVersion;
    private String decision;
    private String reasonCode;
    private String reviewedByDocsId;
    private String workflowRunDocsId;
    private Instant decidedAt;
}
