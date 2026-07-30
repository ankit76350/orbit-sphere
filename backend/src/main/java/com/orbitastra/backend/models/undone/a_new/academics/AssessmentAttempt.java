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

@Document(collection = "assessment_attempts")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_session_student_attempt_uniq",
                def = "{'tenantId':1,'assessmentSessionDocsId':1,'studentDocsId':1,'attemptNo':1}", unique = true),
        @CompoundIndex(name = "tenant_session_mark_status_idx",
                def = "{'tenantId':1,'assessmentSessionDocsId':1,'markingStatus':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAttempt extends AcademicScopedDocument {

    private String assessmentSessionDocsId;
    private String studentDocsId;
    private String enrollmentDocsId;
    private Integer attemptNo;
    private String attendanceStatus;
    private String markingStatus;
    private BigDecimal rawScore;
    private BigDecimal moderatedScore;
    private String gradeCode;
    private String markerDocsId;
    private String moderatorDocsId;
    private Instant markedAt;
    private Instant lockedAt;
    private String responseDocumentDocsId;
    private String malpracticeCaseDocsId;
    private String accommodationPlanDocsId;
}
