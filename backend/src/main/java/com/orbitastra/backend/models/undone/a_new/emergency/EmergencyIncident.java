package com.orbitastra.backend.models.undone.a_new.emergency;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "emergency_incidents")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_emergency_incident_no_uniq",
                def = "{'tenantId':1,'incidentNo':1}", unique = true),
        @CompoundIndex(name = "tenant_campus_emergency_status_idx",
                def = "{'tenantId':1,'campusDocsId':1,'status':1,'activatedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyIncident extends CampusScopedDocument {

    private String incidentNo;
    private String emergencyPlanDocsId;
    private String incidentType;
    private Boolean drill;
    private String severity;
    private String status;
    private String incidentCommanderDocsId;
    private Instant detectedAt;
    private Instant activatedAt;
    private Instant allClearAt;
    private String locationDocsId;
    private String publicSummary;
    private String restrictedReportDocumentDocsId;
    private String postIncidentReviewDocsId;
}
