package com.orbitastra.backend.models.undone.a_new.academics;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "gradebook_records")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_activity_student_grade_uniq",
                def = "{'tenantId':1,'learningActivityDocsId':1,'studentDocsId':1}", unique = true),
        @CompoundIndex(name = "tenant_student_course_idx",
                def = "{'tenantId':1,'studentDocsId':1,'courseOfferingDocsId':1,'publishedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GradebookRecord extends AcademicScopedDocument {

    private String courseOfferingDocsId;
    private String learningActivityDocsId;
    private String studentDocsId;
    private String submissionDocsId;
    private BigDecimal rawScore;
    private BigDecimal maxScore;
    private BigDecimal weightedScore;
    private String gradeCode;
    private String rubricResultDocsId;
    private String markerDocsId;
    private String moderatorDocsId;
    private Instant lockedAt;
    private String lockedByDocsId;
    private Instant publishedAt;
}
