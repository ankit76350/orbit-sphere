package com.orbitastra.backend.models.undone.a_new.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Restricted narrative data is isolated from the case summary so normal case
 * lists never load counselling or safeguarding notes.
 */
@Document(collection = "confidential_case_notes")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_case_note_time_idx",
                def = "{'tenantId':1,'caseType':1,'caseDocsId':1,'recordedAt':-1}"),
        @CompoundIndex(name = "tenant_note_author_time_idx",
                def = "{'tenantId':1,'authorDocsId':1,'recordedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConfidentialCaseNote extends TenantScopedDocument {

    private String caseType;
    private String caseDocsId;
    private String authorDocsId;
    private Instant recordedAt;
    private Confidentiality confidentiality;
    private String encryptedNote;
    private String encryptionKeyVersion;
    private String noteType;
    private Boolean sealed;
    private String sealedByDocsId;
    private Instant sealedAt;

    @Builder.Default
    private List<String> evidenceDocumentDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> permittedPrincipalDocsIds = new ArrayList<>();
}
