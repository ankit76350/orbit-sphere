package com.orbitastra.backend.models.undone.a_new.health;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "immunization_records")
@CompoundIndex(name = "tenant_student_vaccine_dose_uniq",
        def = "{'tenantId':1,'studentDocsId':1,'vaccineCode':1,'doseNo':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImmunizationRecord extends CampusScopedDocument {

    private String studentDocsId;
    private String vaccineCode;
    private String vaccineName;
    private Integer doseNo;
    private LocalDate administeredOn;
    private LocalDate nextDoseDueOn;
    private String providerName;
    private String batchNo;
    private String evidenceDocumentDocsId;
    private String verificationStatus;
    private Confidentiality confidentiality;
}
