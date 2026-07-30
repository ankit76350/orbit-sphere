package com.orbitastra.backend.models.undone.a_new.facilities;

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

@Document(collection = "maintenance_plans")
@CompoundIndex(name = "tenant_maintenance_plan_code_uniq",
        def = "{'tenantId':1,'planCode':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenancePlan extends CampusScopedDocument {

    private String planCode;
    private String name;
    private String assetDocsId;
    private String facilityResourceDocsId;
    private String recurrenceExpression;
    private LocalDate nextDueDate;
    private String assignedTeamKey;
    private String vendorDocsId;
    private Boolean active;

    @Builder.Default
    private List<String> checklistItems = new ArrayList<>();
}
