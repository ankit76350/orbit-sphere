package com.orbitastra.backend.models.undone.a_new.studentlife;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "early_years_care_plans")
@CompoundIndex(name = "tenant_year_student_care_plan_uniq",
        def = "{'tenantId':1,'academicYearDocsId':1,'studentDocsId':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EarlyYearsCarePlan extends AcademicScopedDocument {

    private String studentDocsId;
    private String keyPersonDocsId;
    private Confidentiality confidentiality;
    private LocalDate effectiveFrom;
    private LocalDate reviewDate;
    private String dietaryInstructions;
    private String sleepRoutine;
    private String toiletingInstructions;
    private String comfortAndCommunicationNotes;
    private String consentRecordDocsId;

    @Builder.Default
    private List<String> allergyCodes = new ArrayList<>();

    @Builder.Default
    private List<String> authorizedPickupDocsIds = new ArrayList<>();
}
