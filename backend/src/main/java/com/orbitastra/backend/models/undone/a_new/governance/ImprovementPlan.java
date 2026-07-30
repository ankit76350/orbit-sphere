package com.orbitastra.backend.models.undone.a_new.governance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "improvement_plans")
@CompoundIndex(name = "tenant_campus_plan_code_uniq",
        def = "{'tenantId':1,'campusDocsId':1,'planCode':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImprovementPlan extends CampusScopedDocument {

    private String planCode;
    private String title;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private ApprovalState state;
    private String ownerDocsId;

    @Builder.Default
    private List<Objective> objectives = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Objective {
        private String objectiveKey;
        private String description;
        private String metricKey;
        private String baseline;
        private String target;
        private String ownerDocsId;
        private LocalDate dueDate;
        private String currentStatus;
    }
}
