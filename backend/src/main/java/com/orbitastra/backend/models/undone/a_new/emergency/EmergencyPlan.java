package com.orbitastra.backend.models.undone.a_new.emergency;

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

@Document(collection = "emergency_plans")
@CompoundIndex(name = "tenant_campus_emergency_plan_version_uniq",
        def = "{'tenantId':1,'campusDocsId':1,'planCode':1,'planVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyPlan extends CampusScopedDocument {

    private String planCode;
    private Integer planVersion;
    private String emergencyType;
    private String title;
    private ApprovalState state;
    private LocalDate effectiveFrom;
    private LocalDate reviewDate;
    private String incidentCommanderPositionDocsId;
    private String procedureDocumentDocsId;
    private String communicationTemplateDocsId;

    @Builder.Default
    private List<String> assemblyPointDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> responseTeamPositionDocsIds = new ArrayList<>();
}
