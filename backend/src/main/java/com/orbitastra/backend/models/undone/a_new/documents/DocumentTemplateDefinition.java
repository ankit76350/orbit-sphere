package com.orbitastra.backend.models.undone.a_new.documents;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "document_template_definitions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_document_template_key_version_uniq",
                def = "{'tenantId':1,'templateKey':1,'templateVersion':1}", unique = true),
        @CompoundIndex(name = "tenant_document_template_status_effective_idx",
                def = "{'tenantId':1,'status':1,'effectiveFrom':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplateDefinition extends TenantScopedDocument {

    private String templateKey;
    private Integer templateVersion;
    private String name;
    private String category;
    private String rendererType;
    private String templateStoredObjectDocsId;
    private String schemaJson;
    private String locale;
    private String status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private String approvedByDocsId;
    private String supersedesTemplateDocsId;

    @Builder.Default
    private List<String> requiredDataScopes = new ArrayList<>();
}
