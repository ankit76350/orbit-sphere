package com.orbitastra.backend.models.undone.a_new.security;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "evidence_media")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_evidence_no_uniq",
                def = "{'tenantId':1,'evidenceNo':1}", unique = true),
        @CompoundIndex(name = "tenant_security_event_evidence_idx",
                def = "{'tenantId':1,'securityEventDocsId':1,'capturedFrom':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceMedia extends CampusScopedDocument {

    private String evidenceNo;
    private String securityEventDocsId;
    private String securityDeviceDocsId;
    private String storedObjectDocsId;
    private Instant capturedFrom;
    private Instant capturedUntil;
    private Confidentiality confidentiality;
    private String integrityHash;
    private String redactedObjectDocsId;
    private String chainOfCustodyStatus;
    private String retentionRuleDocsId;
    private String legalHoldDocsId;
}
