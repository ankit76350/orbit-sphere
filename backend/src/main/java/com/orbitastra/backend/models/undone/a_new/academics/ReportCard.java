package com.orbitastra.backend.models.undone.a_new.academics;

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

@Document(collection = "report_cards")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_report_card_no_uniq",
                def = "{'tenantId':1,'reportCardNo':1}", unique = true),
        @CompoundIndex(name = "tenant_student_period_version_uniq",
                def = "{'tenantId':1,'studentDocsId':1,'reportingPeriodDocsId':1,'reportVersion':1}", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCard extends AcademicScopedDocument {

    private String reportCardNo;
    private String studentDocsId;
    private String enrollmentDocsId;
    private String reportingPeriodDocsId;
    private Integer reportVersion;
    private String status;
    private Instant lockedAt;
    private String lockedByDocsId;
    private Instant publishedAt;
    private String publishedByDocsId;
    private String generatedDocumentDocsId;
    private String teacherRemark;
    private String aiHumanReviewDocsId;

    @Builder.Default
    private List<SubjectResult> subjectResults = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectResult {
        private String subjectNodeDocsId;
        private String gradeCode;
        private String scoreDisplay;
        private String attainmentLevel;
        private String teacherRemark;
    }
}
