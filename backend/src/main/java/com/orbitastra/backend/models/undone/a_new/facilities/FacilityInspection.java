package com.orbitastra.backend.models.undone.a_new.facilities;

import java.time.Instant;
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

@Document(collection = "facility_inspections")
@CompoundIndex(name = "tenant_resource_inspection_due_idx",
        def = "{'tenantId':1,'facilityResourceDocsId':1,'inspectionType':1,'nextDueDate':1}")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityInspection extends CampusScopedDocument {

    private String facilityResourceDocsId;
    private String assetDocsId;
    private String inspectionType;
    private String inspectorDocsId;
    private Instant inspectedAt;
    private String outcome;
    private LocalDate certificateValidUntil;
    private LocalDate nextDueDate;
    private String certificateDocumentDocsId;

    @Builder.Default
    private List<Finding> findings = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private String severity;
        private String description;
        private String workOrderDocsId;
        private Boolean resolved;
    }
}
