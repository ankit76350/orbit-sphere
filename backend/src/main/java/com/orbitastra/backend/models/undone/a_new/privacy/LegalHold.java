package com.orbitastra.backend.models.undone.a_new.privacy;

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

@Document(collection = "legal_holds")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_legal_hold_no_uniq",
                def = "{'tenantId':1,'holdNo':1}", unique = true),
        @CompoundIndex(name = "tenant_hold_status_idx",
                def = "{'tenantId':1,'status':1,'releasedAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LegalHold extends TenantScopedDocument {

    private String holdNo;
    private String title;
    private String reason;
    private String legalCaseDocsId;
    private String status;
    private String authorizedByDocsId;
    private Instant effectiveAt;
    private Instant releasedAt;
    private String releasedByDocsId;

    @Builder.Default
    private List<String> subjectDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> collectionNames = new ArrayList<>();
}
