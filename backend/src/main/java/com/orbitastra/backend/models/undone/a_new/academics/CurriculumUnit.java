package com.orbitastra.backend.models.undone.a_new.academics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;
import com.orbitastra.backend.models.undone.a_new.common.PlatformEnums.ApprovalState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "curriculum_units")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_year_unit_code_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'unitCode':1}", unique = true),
        @CompoundIndex(name = "tenant_year_subject_state_idx",
                def = "{'tenantId':1,'academicYearDocsId':1,'subjectNodeDocsId':1,'state':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumUnit extends AcademicScopedDocument {

    private String unitCode;
    private String title;
    private String gradeNodeDocsId;
    private String subjectNodeDocsId;
    private Integer sequence;
    private Integer plannedMinutes;
    private ApprovalState state;
    private Instant approvedAt;
    private String approvedByDocsId;
    private String inquiryStatement;
    private String assessmentStrategy;
    private String differentiationNotes;

    @Builder.Default
    private List<String> learningOutcomeDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> prerequisiteUnitDocsIds = new ArrayList<>();
}
