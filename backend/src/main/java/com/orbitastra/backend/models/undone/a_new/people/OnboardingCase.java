package com.orbitastra.backend.models.undone.a_new.people;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "onboarding_cases")
@CompoundIndex(name = "tenant_staff_onboarding_uniq",
        def = "{'tenantId':1,'staffDocsId':1,'joiningDate':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCase extends CampusScopedDocument {

    private String recruitmentApplicationDocsId;
    private String staffDocsId;
    private String employmentRecordDocsId;
    private LocalDate joiningDate;
    private String ownerDocsId;
    private String status;
    private LocalDate probationReviewDate;

    @Builder.Default
    private List<OnboardingTask> tasks = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingTask {
        private String taskKey;
        private String title;
        private String assigneeDocsId;
        private LocalDate dueDate;
        private String status;
        private String evidenceDocumentDocsId;
    }
}
