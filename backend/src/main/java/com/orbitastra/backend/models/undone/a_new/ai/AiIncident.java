package com.orbitastra.backend.models.undone.a_new.ai;

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

@Document(collection = "ai_incidents")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_ai_incident_no_uniq",
                def = "{'tenantId':1,'incidentNo':1}", unique = true),
        @CompoundIndex(name = "tenant_ai_severity_status_idx",
                def = "{'tenantId':1,'severity':1,'status':1,'detectedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiIncident extends TenantScopedDocument {

    private String incidentNo;
    private String aiUseCaseDocsId;
    private String aiRunDocsId;
    private String incidentType;
    private String severity;
    private String status;
    private Instant detectedAt;
    private String ownerDocsId;
    private Boolean killSwitchActivated;
    private String impactSummary;
    private String rootCause;
    private String remediation;
    private String privacyIncidentDocsId;

    @Builder.Default
    private List<String> affectedRunDocsIds = new ArrayList<>();
}
