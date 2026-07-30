package com.orbitastra.backend.models.undone.a_new.admissions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "admission_cycles")
@CompoundIndex(name = "tenant_campus_cycle_code_uniq",
        def = "{'tenantId':1,'campusDocsId':1,'cycleCode':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionCycle extends AcademicScopedDocument {

    private String cycleCode;
    private String name;
    private Instant inquiryOpenAt;
    private Instant applicationOpenAt;
    private Instant applicationCloseAt;
    private Instant enrollmentDeadlineAt;
    private String status;
    private String applicationFormDocsId;
    private String workflowDefinitionDocsId;

    @Builder.Default
    private List<IntakeCapacity> capacities = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntakeCapacity {
        private String gradeNodeDocsId;
        private Integer totalSeats;
        private Integer reservedSeats;
        private Integer offeredSeats;
        private Integer enrolledSeats;
    }
}
