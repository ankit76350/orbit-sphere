package com.orbitastra.backend.models.undone.a_new.health;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "health_profiles")
@CompoundIndex(name = "tenant_student_health_profile_uniq",
        def = "{'tenantId':1,'studentDocsId':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfile extends CampusScopedDocument {

    private String studentDocsId;
    private Confidentiality confidentiality;
    private String bloodGroupCode;
    private String encryptedClinicalSummary;
    private String emergencyCarePlanDocumentDocsId;
    private String primaryDoctorContact;
    private String insurerReference;
    private String consentRecordDocsId;
    private Instant verifiedAt;
    private String verifiedByDocsId;

    @Builder.Default
    private List<CodedAlert> alerts = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodedAlert {
        private String alertType;
        private String codeSystem;
        private String code;
        private String display;
        private String severity;
        private String encryptedInstructions;
    }
}
