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

@Document(collection = "career_plans")
@CompoundIndex(name = "tenant_year_student_career_uniq",
        def = "{'tenantId':1,'academicYearDocsId':1,'studentDocsId':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CareerPlan extends AcademicScopedDocument {

    private String studentDocsId;
    private String counsellorDocsId;
    private Confidentiality confidentiality;
    private String interestProfile;
    private String aptitudeAssessmentDocsId;
    private String targetPathway;
    private LocalDate nextReviewDate;

    @Builder.Default
    private List<String> targetCourseCodes = new ArrayList<>();

    @Builder.Default
    private List<String> actionItems = new ArrayList<>();
}
