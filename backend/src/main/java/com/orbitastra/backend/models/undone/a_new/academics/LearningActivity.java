package com.orbitastra.backend.models.undone.a_new.academics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "learning_activities")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_course_activity_code_uniq",
                def = "{'tenantId':1,'courseOfferingDocsId':1,'activityCode':1}", unique = true),
        @CompoundIndex(name = "tenant_course_publish_due_idx",
                def = "{'tenantId':1,'courseOfferingDocsId':1,'publishedAt':-1,'dueAt':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearningActivity extends AcademicScopedDocument {

    public enum ActivityType {
        RESOURCE,
        ASSIGNMENT,
        QUIZ,
        DISCUSSION,
        PROJECT,
        OBSERVATION,
        EXTERNAL_TOOL
    }

    private String courseOfferingDocsId;
    private String curriculumUnitDocsId;
    private String activityCode;
    private ActivityType type;
    private String title;
    private String instructions;
    private Instant openAt;
    private Instant dueAt;
    private Instant closeAt;
    private BigDecimal maxScore;
    private String rubricDocsId;
    private Boolean allowResubmission;
    private Integer maxAttempts;
    private Instant publishedAt;

    @Builder.Default
    private List<String> learningOutcomeDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();

    @Builder.Default
    private List<String> assessmentItemDocsIds = new ArrayList<>();
}
