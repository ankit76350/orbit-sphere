package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.Confidentiality;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "accommodation_plans")
@CompoundIndex(name = "tenant_year_student_accommodation_idx",
        def = "{'tenantId':1,'academicYearDocsId':1,'studentDocsId':1,'state':1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationPlan extends AcademicScopedDocument {

    private String studentDocsId;
    private String inclusionPlanDocsId;
    private ApprovalState state;
    private Confidentiality confidentiality;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Boolean classroomApplicable;
    private Boolean examinationApplicable;
    private String approvedByDocsId;

    @Builder.Default
    private List<Accommodation> accommodations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Accommodation {
        private String code;
        private String description;
        private Integer extraTimePercent;
        private String resourceDocsId;
    }
}
