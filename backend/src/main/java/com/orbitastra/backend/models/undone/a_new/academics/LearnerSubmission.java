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

@Document(collection = "learner_submissions")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_activity_student_attempt_uniq",
                def = "{'tenantId':1,'learningActivityDocsId':1,'studentDocsId':1,'attemptNo':1}", unique = true),
        @CompoundIndex(name = "tenant_course_status_submitted_idx",
                def = "{'tenantId':1,'courseOfferingDocsId':1,'status':1,'submittedAt':-1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerSubmission extends AcademicScopedDocument {

    public enum SubmissionStatus {
        DRAFT,
        SUBMITTED,
        LATE,
        RETURNED,
        RESUBMISSION_REQUIRED,
        GRADED,
        EXEMPT
    }

    private String courseOfferingDocsId;
    private String learningActivityDocsId;
    private String studentDocsId;
    private Integer attemptNo;
    private SubmissionStatus status;
    private String responseText;

    @Builder.Default
    private List<String> attachmentDocsIds = new ArrayList<>();

    private Instant submittedAt;
    private String gradedByDocsId;
    private Instant gradedAt;
    private BigDecimal score;
    private String feedback;
    private String plagiarismCheckDocsId;
}
