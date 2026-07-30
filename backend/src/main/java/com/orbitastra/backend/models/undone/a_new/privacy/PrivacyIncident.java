package com.orbitastra.backend.models.undone.a_new.privacy;

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

@Document(collection = "privacy_incidents")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_privacy_incident_no_uniq",
                def = "{'tenantId':1,'incidentNo':1}", unique = true),
        @CompoundIndex(name = "tenant_privacy_status_deadline_idx",
                def = "{'tenantId':1,'status':1,'notificationDeadline':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyIncident extends TenantScopedDocument {

    private String incidentNo;
    private String title;
    private String status;
    private String severity;
    private Confidentiality confidentiality;
    private Instant detectedAt;
    private Instant containedAt;
    private String ownerDocsId;
    private Integer estimatedSubjectsAffected;
    private Boolean childrenAffected;
    private Boolean regulatorNotificationRequired;
    private Boolean subjectNotificationRequired;
    private Instant notificationDeadline;
    private String rootCause;
    private String remediationPlan;
    private String incidentReportDocumentDocsId;

    @Builder.Default
    private List<String> affectedDataCategoryKeys = new ArrayList<>();
}
