package com.orbitastra.backend.models.undone.a_new.privacy;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "consent_records")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_subject_purpose_version_uniq",
                def = "{'tenantId':1,'subjectType':1,'subjectDocsId':1,'purposeKey':1,'consentVersion':1}",
                unique = true),
        @CompoundIndex(name = "tenant_subject_purpose_status_idx",
                def = "{'tenantId':1,'subjectDocsId':1,'purposeKey':1,'status':1,'validUntil':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRecord extends TenantScopedDocument {

    public enum ConsentStatus {
        REQUESTED,
        GRANTED,
        REFUSED,
        WITHDRAWN,
        EXPIRED,
        NOT_REQUIRED
    }

    private PersonType subjectType;
    private String subjectDocsId;
    private String decisionMakerDocsId;
    private String relationshipToSubject;
    private String purposeKey;
    private Integer consentVersion;
    private String privacyNoticeDocsId;
    private ConsentStatus status;
    private String captureChannel;
    private Instant requestedAt;
    private Instant decidedAt;
    private Instant validUntil;
    private Instant withdrawnAt;
    private String withdrawalReason;
    private String evidenceDocumentDocsId;
    private String proofHash;
}
