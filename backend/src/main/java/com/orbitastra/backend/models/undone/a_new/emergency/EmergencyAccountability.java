package com.orbitastra.backend.models.undone.a_new.emergency;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "emergency_accountability")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_incident_person_uniq",
                def = "{'tenantId':1,'emergencyIncidentDocsId':1,'personType':1,'personDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_incident_status_location_idx",
                def = "{'tenantId':1,'emergencyIncidentDocsId':1,'accountabilityStatus':1,'assemblyPointDocsId':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAccountability extends CampusScopedDocument {

    private String emergencyIncidentDocsId;
    private PersonType personType;
    private String personDocsId;
    private String accountabilityStatus;
    private String assemblyPointDocsId;
    private String recordedByDocsId;
    private Instant recordedAt;
    private String reunificationStatus;
    private String releasedToDocsId;
    private Instant releasedAt;
    private String exceptionNotes;
}
