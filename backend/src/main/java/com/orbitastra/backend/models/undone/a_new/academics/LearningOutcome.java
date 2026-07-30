package com.orbitastra.backend.models.undone.a_new.academics;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "learning_outcomes")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_framework_outcome_code_uniq",
                def = "{'tenantId':1,'frameworkDocsId':1,'outcomeCode':1}", unique = true),
        @CompoundIndex(name = "tenant_outcome_subject_grade_idx",
                def = "{'tenantId':1,'subjectNodeDocsId':1,'gradeNodeDocsId':1,'sequence':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningOutcome extends CampusScopedDocument {

    private String frameworkDocsId;
    private String parentOutcomeDocsId;
    private String outcomeCode;
    private String title;
    private String description;
    private String subjectNodeDocsId;
    private String gradeNodeDocsId;
    private String strand;
    private Integer sequence;
    private String cognitiveLevel;

    @Builder.Default
    private List<String> competencyTags = new ArrayList<>();
}
