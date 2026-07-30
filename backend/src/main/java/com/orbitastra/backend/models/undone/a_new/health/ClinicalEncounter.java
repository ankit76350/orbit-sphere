package com.orbitastra.backend.models.undone.a_new.health;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "clinical_encounters")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_encounter_no_uniq",
                def = "{'tenantId':1,'encounterNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_encounter_time_idx",
                def = "{'tenantId':1,'studentDocsId':1,'startedAt':-1}"),
        @CompoundIndex(name = "tenant_clinician_status_time_idx",
                def = "{'tenantId':1,'clinicianDocsId':1,'status':1,'startedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalEncounter extends CampusScopedDocument {

    private String encounterNo;
    private String studentDocsId;
    private String clinicianDocsId;
    private Instant startedAt;
    private Instant endedAt;
    private String encounterType;
    private String status;
    private Confidentiality confidentiality;
    private String encryptedClinicalNote;
    private String disposition;
    private Boolean guardianNotified;
    private Instant guardianNotifiedAt;
    private String externalReferralDocsId;

    @Builder.Default
    private List<String> codedSymptoms = new ArrayList<>();

    @Builder.Default
    private List<String> codedDiagnoses = new ArrayList<>();
}
