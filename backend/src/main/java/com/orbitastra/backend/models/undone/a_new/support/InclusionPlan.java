package com.orbitastra.backend.models.undone.a_new.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
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

@Document(collection = "inclusion_plans")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_inclusion_plan_no_uniq",
                def = "{'tenantId':1,'planNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_year_inclusion_idx",
                def = "{'tenantId':1,'studentDocsId':1,'academicYearDocsId':1,'state':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InclusionPlan extends AcademicScopedDocument {

    private String planNo;
    private String studentDocsId;
    private String planType;
    private ApprovalState state;
    private Confidentiality confidentiality;
    private String coordinatorDocsId;
    private LocalDate effectiveFrom;
    private LocalDate nextReviewDate;
    private String guardianConsentDocsId;
    private String studentParticipationNotes;
    private String transitionPlan;

    @Builder.Default
    private List<Goal> goals = new ArrayList<>();

    @Builder.Default
    private List<String> serviceCodes = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Goal {
        private String goalKey;
        private String description;
        private String baseline;
        private String target;
        private String measurementMethod;
        private LocalDate targetDate;
        private String status;
    }
}
