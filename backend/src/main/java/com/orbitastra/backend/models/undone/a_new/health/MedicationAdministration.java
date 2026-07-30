package com.orbitastra.backend.models.undone.a_new.health;

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

@Document(collection = "medication_administrations")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_student_med_time_idx",
                def = "{'tenantId':1,'studentDocsId':1,'scheduledAt':-1}"),
        @CompoundIndex(name = "tenant_clinician_admin_time_idx",
                def = "{'tenantId':1,'administeredByDocsId':1,'administeredAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationAdministration extends CampusScopedDocument {

    private String studentDocsId;
    private String clinicalEncounterDocsId;
    private String healthProfileDocsId;
    private String medicationCode;
    private String medicationName;
    private String dose;
    private String route;
    private Instant scheduledAt;
    private Instant administeredAt;
    private String administeredByDocsId;
    private String status;
    private String reasonNotAdministered;
    private String guardianConsentDocsId;
    private Confidentiality confidentiality;
}
